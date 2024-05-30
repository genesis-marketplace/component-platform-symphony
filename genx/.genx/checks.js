const { serverFolderExists, buildGradleExists, matchingMajorVersionFUI, matchingMajorVersionGSF } = require('@genesislcap/seed-utils');

module.exports = [
    serverFolderExists,
    buildGradleExists,
    matchingMajorVersionGSF
]
