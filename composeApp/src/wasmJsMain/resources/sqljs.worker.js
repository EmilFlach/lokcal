importScripts("./sql-wasm.js");

let db = null;
let SQL = null;
let saveTimeout = null;
const SAVE_DELAY = 1000;

const DB_NAME = "lokcal_db";
const STORE_NAME = "sqlite_data";
const KEY = "database";

function openIndexedDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME);
            }
        };
        request.onsuccess = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                // If the store is missing but no upgrade was triggered, we need to bump the version.
                const newVersion = db.version + 1;
                db.close();
                const upgradeRequest = indexedDB.open(DB_NAME, newVersion);
                upgradeRequest.onupgradeneeded = (e) => {
                    const upgradeDb = e.target.result;
                    if (!upgradeDb.objectStoreNames.contains(STORE_NAME)) {
                        upgradeDb.createObjectStore(STORE_NAME);
                    }
                };
                upgradeRequest.onsuccess = (e) => resolve(e.target.result);
                upgradeRequest.onerror = (e) => reject(e.target.error);
            } else {
                resolve(db);
            }
        };
        request.onerror = (event) => reject(event.target.error);
    });
}

async function loadFromIndexedDB() {
    try {
        const idb = await openIndexedDB();
        return new Promise((resolve, reject) => {
            const transaction = idb.transaction(STORE_NAME, "readonly");
            const store = transaction.objectStore(STORE_NAME);
            const request = store.get(KEY);
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    } catch (e) {
        console.error("Failed to load from IndexedDB", e);
        return null;
    }
}

async function saveToIndexedDB() {
    if (!db) return;
    if (saveTimeout) {
        clearTimeout(saveTimeout);
        saveTimeout = null;
    }
    try {
        const buffer = db.export();
        const idb = await openIndexedDB();
        return new Promise((resolve, reject) => {
            const transaction = idb.transaction(STORE_NAME, "readwrite");
            const store = transaction.objectStore(STORE_NAME);
            const request = store.put(buffer, KEY);
            request.onsuccess = () => {
                console.log("Database saved to IndexedDB.");
                resolve();
            };
            request.onerror = () => reject(request.error);
        });
    } catch (e) {
        console.error("Failed to save to IndexedDB", e);
    }
}

function scheduleSave() {
    if (saveTimeout) clearTimeout(saveTimeout);
    saveTimeout = setTimeout(saveToIndexedDB, SAVE_DELAY);
}

async function createDatabase() {
    console.log("Initializing SQL.js...");
    SQL = await initSqlJs({ locateFile: file => {
        console.log("Locating file:", file);
        return `./${file}`;
    }});

    const savedBuffer = await loadFromIndexedDB();
    if (savedBuffer) {
        db = new SQL.Database(new Uint8Array(savedBuffer));
        console.log("SQL.js initialized, database loaded from IndexedDB.");
    } else {
        db = new SQL.Database();
        console.log("SQL.js initialized, new database created.");
    }
}

async function handleAction(data) {
    try {
        switch (data && data.action) {
            case "exec":
                if (!data["sql"]) {
                    throw new Error("exec: Missing query string");
                }

                const res = db.exec(data.sql, data.params);
                let results = { values: [] };
                if (res.length > 0) {
                    const lastRes = res[res.length - 1];
                    results = {
                        columns: lastRes.columns,
                        values: lastRes.values
                    };
                }
                
                if (!data.sql.trim().toUpperCase().startsWith("SELECT")) {
                    scheduleSave();
                }

                postMessage({
                    id: data.id,
                    results: results
                });
                break;
            case "begin_transaction":
                db.exec("BEGIN TRANSACTION;");
                postMessage({
                    id: data.id,
                    results: { values: [] }
                });
                break;
            case "end_transaction":
                db.exec("COMMIT;");
                postMessage({
                    id: data.id,
                    results: { values: [] }
                });
                scheduleSave();
                break;
            case "rollback_transaction":
                db.exec("ROLLBACK;");
                postMessage({
                    id: data.id,
                    results: { values: [] }
                });
                break;
            case "export_db":
                await saveToIndexedDB();
                postMessage({
                    id: data.id,
                    buffer: db.export()
                });
                break;
            case "import_db":
                if (!data["buffer"]) {
                    throw new Error("import_db: Missing database buffer");
                }
                db.close();
                db = new SQL.Database(data.buffer);
                await saveToIndexedDB();
                postMessage({
                    id: data.id
                });
                break;
            default:
                throw new Error(`Unsupported action: ${data && data.action}`);
        }
    } catch (e) {
        console.error("Error in worker action:", e);
        postMessage({
            id: data.id,
            error: e.message
        });
    }
}

let messageQueue = Promise.resolve();

function onMessage(event) {
    messageQueue = messageQueue.then(() => handleAction(event.data)).catch(err => {
        console.error("Worker unhandled error in queue:", err);
    });
}

if (typeof importScripts === "function") {
    db = null;
    const sqlModuleReady = createDatabase();
    self.onmessage = (event) => {
        messageQueue = messageQueue.then(() => sqlModuleReady).then(() => handleAction(event.data)).catch(err => {
             console.error("Worker unhandled error:", err);
             postMessage({
                id: event.data.id,
                error: err.message || err
            });
        });
    }
}