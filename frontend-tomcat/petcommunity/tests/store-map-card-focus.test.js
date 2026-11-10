const assert = require("assert");
const fs = require("fs");
const path = require("path");
const vm = require("vm");

const appPath = path.join(__dirname, "..", "assets", "app.js");
let source = fs.readFileSync(appPath, "utf8");
source = source.replace(/\}\)\(\);\s*$/, [
    "window.__storeMapTest = {",
    "    focusNearbyStoreCard: focusNearbyStoreCard",
    "};",
    "})();"
].join("\n"));

const classes = new Set();
let selected = "";
let scrollOptions = null;
let timeoutDelay = null;
let timeoutCallback = null;
let clearedTimer = null;
const card = {
    offsetWidth: 320,
    classList: {
        add(name) { classes.add(name); },
        remove(name) { classes.delete(name); }
    },
    scrollIntoView(options) { scrollOptions = options; }
};

const context = {
    console,
    URLSearchParams,
    localStorage: {
        getItem() { return null; },
        setItem() {},
        removeItem() {}
    },
    document: {
        body: {},
        addEventListener() {},
        getElementById() { return null; },
        querySelector(selector) {
            selected = selector;
            return card;
        }
    },
    window: {
        location: {search: ""},
        setTimeout(callback, delay) {
            timeoutCallback = callback;
            timeoutDelay = delay;
            return 91;
        },
        clearTimeout(timer) { clearedTimer = timer; }
    }
};
context.window.window = context.window;
context.window.document = context.document;
context.window.localStorage = context.localStorage;

vm.runInNewContext(source, context, {filename: appPath});
context.window.__storeMapTest.focusNearbyStoreCard(42);

assert.strictEqual(selected, '[data-store-id="42"]');
assert.strictEqual(scrollOptions.behavior, "smooth");
assert.strictEqual(scrollOptions.block, "center");
assert.strictEqual(classes.has("store-card-highlight"), true);
assert.strictEqual(timeoutDelay, 5000);

context.window.__storeMapTest.focusNearbyStoreCard(42);
assert.strictEqual(clearedTimer, 91);

timeoutCallback();
assert.strictEqual(classes.has("store-card-highlight"), false);
console.log("Store map card focus behavior passed.");
