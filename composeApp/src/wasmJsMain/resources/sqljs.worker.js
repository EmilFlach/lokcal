importScripts("./sql-wasm.js");

let db = null;
let SQL = null;
async function createDatabase() {
    console.log("Initializing SQL.js...");
    SQL = await initSqlJs({ locateFile: file => {
        console.log("Locating file:", file);
        return `./${file}`;
    }});
    db = new SQL.Database();
    console.log("SQL.js initialized, database created.");
}

async function onModuleReady() {
    const data = this.data;

    switch (data && data.action) {
        case "exec":
            if (!data["sql"]) {
                throw new Error("exec: Missing query string");
            }

            return postMessage({
                id: data.id,
                results: db.exec(data.sql, data.params)[0] ?? { values: [] }
            });
        case "begin_transaction":
            return postMessage({
                id: data.id,
                results: db.exec("BEGIN TRANSACTION;")
            })
        case "end_transaction":
            return postMessage({
                id: data.id,
                results: db.exec("END TRANSACTION;")
            })
        case "rollback_transaction":
            return postMessage({
                id: data.id,
                results: db.exec("ROLLBACK TRANSACTION;")
            })
        case "export_db":
            return postMessage({
                id: data.id,
                buffer: db.export()
            });
        case "import_db":
            if (!data["buffer"]) {
                throw new Error("import_db: Missing database buffer");
            }
            db.close();
            db = new SQL.Database(data.buffer);
            return postMessage({
                id: data.id
            });
        default:
            throw new Error(`Unsupported action: ${data && data.action}`);
    }
}

function onError(err) {
    return postMessage({
        id: this.data.id,
        error: err
    });
}

if (typeof importScripts === "function") {
    db = null;
    const sqlModuleReady = createDatabase()
    self.onmessage = (event) => {
        return sqlModuleReady
            .then(onModuleReady.bind(event))
            .catch(onError.bind(event));
    }
}