// Сборка пакета: ESM-бандл без react внутри, .d.ts от tsc и стили рядом.
import { build } from "esbuild";
import { execSync } from "node:child_process";
import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";

rmSync("dist", { recursive: true, force: true });
mkdirSync("dist", { recursive: true });
await build({
  entryPoints: ["src/index.ts"],
  bundle: true,
  format: "esm",
  jsx: "automatic",
  target: "es2020",
  external: ["react", "react-dom", "react/jsx-runtime"],
  outfile: "dist/index.js",
});
execSync("npx tsc -p tsconfig.json", { stdio: "inherit" });
// Токены встраиваются в styles.css: потребитель получает один файл без относительных @import,
// которые теряются при копировании стилей в чужой проект. Шрифт — локальный (fonts.css рядом):
// Google Fonts из РФ грузится нестабильно, а дизайну нужен ровно тот Roboto, что на устройстве.
const styles = readFileSync("src/styles.css", "utf8").replace('@import "./tokens.css";', "");
writeFileSync("dist/styles.css", `${readFileSync("src/tokens.css", "utf8")}
${styles}`);
cpSync("src/fonts.css", "dist/fonts.css");
cpSync("fonts", "dist/fonts", { recursive: true });
cpSync("src/tokens.css", "dist/tokens.css");
console.log("built dist/");
