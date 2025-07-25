import { defineConfig } from 'vite';
import path from 'path';
const scalaVersion = '3.7.1'
const scalaProjectName = 'tictactoetyrianclient'

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
      host: "0.0.0.0",
      historyApiFallback: true
    },
  };
});
