// macOS App Data protection denies a Firefox launched by another app (IDE, terminal) its own
// ~/Library/Application Support/Firefox, and Firefox then exits with "Could not find profile folder".
// Karma spawns Firefox with this process's environment, so Firefox gets a data root of its own.
if (process.platform === "darwin") {
    const path = require("path");
    process.env.MOZ_APP_DATA = path.join(config.basePath, "firefox-data", "app");
    process.env.MOZ_LOCAL_APP_DATA = path.join(config.basePath, "firefox-data", "local");
}
