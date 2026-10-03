// Karma's Mocha default is 2 s; the Node test task runs with 10 s (kommons-js-conventions).
config.set({
    client: {
        mocha: {
            timeout: 10000,
        },
    },
});
