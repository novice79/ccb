module.exports = {
    productionSourceMap: false,
    outputDir : "../resources/public",
    baseUrl : process.env.LOCAL_TEST !== 'here' ? '/ccb/' : '/'
}