const fs = require("node:fs");

/** @type {Set<string>} */
const dirs = new Set();

const files = fs.readdirSync(".", {encoding: "utf-8", recursive: false, withFileTypes: true})
  .filter((de) => {
    if (de.isDirectory()) {
      dirs.add(de.name);
      return false;
    }
    return de.isFile() && !de.name.startsWith(".") && (de.name.endsWith(".m4b") || de.name.endsWith(".m4a") || de.name.endsWith(".mp3"));
  })
  .map((de) => de.name)
  .sort();

const books = files.reduce((b, f) => {
  const base = f.replace(/(?:\s+\d+|\[File \d+ of \d+])?\.(?:mp3|m4[ab])$/, "");
  if (base !== f && !dirs.has(base)) {
    b[base] ??= [];
    b[base].push(f);
  }
  return b;
}, /** @type {Record<string, string[]>} */ {});

for (const [base, /** @type {string[]} */ names] of Object.entries(books)) {
  fs.mkdirSync(base, {recursive: true});
  for (const name of names) {
    fs.renameSync(name, `${base}/${name}`);
  }
}
