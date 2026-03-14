const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: path.resolve(__dirname, '../../../../build/wasm/node_modules/sql.js/dist/sql-wasm.js'),
                to: '.'
            },
            {
                from: path.resolve(__dirname, '../../../../build/wasm/node_modules/sql.js/dist/sql-wasm.wasm'),
                to: '.'
            }
        ]
    })
);
