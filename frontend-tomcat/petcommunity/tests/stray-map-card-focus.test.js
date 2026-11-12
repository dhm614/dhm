const assert = require("assert");
const fs = require("fs");
const path = require("path");
const vm = require("vm");

const appPath = path.join(__dirname, "..", "assets", "app.js");
let source = fs.readFileSync(appPath, "utf8");
source = source.replace(/\}\)\(\);\s*$/, [
    "window.__strayMapTest = {",
    "    focusStrayAnimalCard: focusStrayAnimalCard",
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
            return 37;
        },
        clearTimeout(timer) { clearedTimer = timer; }
    }
};
context.window.window = context.window;
context.window.document = context.document;
context.window.localStorage = context.localStorage;

vm.runInNewContext(source, context, {filename: appPath});
context.window.__strayMapTest.focusStrayAnimalCard(42);

assert.strictEqual(selected, '[data-stray-animal-id="42"]');
assert.strictEqual(scrollOptions.behavior, "smooth");
assert.strictEqual(scrollOptions.block, "center");
assert.strictEqual(classes.has("stray-animal-card-highlight"), true);
assert.strictEqual(timeoutDelay, 5000);

context.window.__strayMapTest.focusStrayAnimalCard(42);
assert.strictEqual(clearedTimer, 37);

timeoutCallback();
assert.strictEqual(classes.has("stray-animal-card-highlight"), false);
console.log("Stray map card focus behavior passed.");
