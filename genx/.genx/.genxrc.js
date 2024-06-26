module.exports = {
  details: () => ({
    nextStepsMessage: `
Genesis Symphony PBC has been added successfully 🎉

> Go into the (web) client directory with \`cd client\`

> Install the PBC dependencies with \`npm run bootstrap\`

> Start the development server with \`npm run dev\`
`
  }),
  prompts: () => () => {},
  configure: () => require('./configure'),
};
