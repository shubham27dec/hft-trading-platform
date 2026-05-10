function fn() {
    var config = {};
    config.baseUrl = karate.properties['baseUrl'] || 'http://localhost:8080';
    config.testApiKey = 'test-api-key-001';
    return config;
}
