import { defineConfig } from 'vite';
import path from 'path';
const scalaVersion = '3.6.3'
const scalaProjectName = 'tictactoeclient2'

export default defineConfig(({ command }) => {
  return {
    resolve: {
      alias: {
        "scalajs": command == "serve" ? `./target/scala-${scalaVersion}/${scalaProjectName}-fastopt/main.js` : `./target/scala-${scalaVersion}/${scalaProjectName}-opt/main.js`,
        "resources": path.resolve(__dirname, "./src/main/resources"),
        "js": path.resolve(__dirname, "./src/main/js"),
      }
    },
    server: {
      port: 9876,
      historyApiFallback: true
    },
  };
});
