Feature: Order Entry API

  Background:
    * url baseUrl
    * header X-API-Key = testApiKey

  Scenario: Valid market order is accepted with status SUBMITTED
    Given path '/api/orders'
    And request { clientOrderId: 'karate-order-1', symbol: 'AAPL', side: 'BUY', type: 'MARKET', quantity: 100, limitPrice: 0 }
    When method POST
    Then status 202
    And match response.orderId == '#notnull'
    And match response.status == 'SUBMITTED'
    And match response.symbol == 'AAPL'
    And match response.clientOrderId == 'karate-order-1'

  Scenario: Valid limit order is accepted
    Given path '/api/orders'
    And request { clientOrderId: 'karate-order-2', symbol: 'TSLA', side: 'SELL', type: 'LIMIT', quantity: 50, limitPrice: 250.0 }
    When method POST
    Then status 202
    And match response.status == 'SUBMITTED'

  Scenario: Missing required fields returns 400
    Given path '/api/orders'
    And request { clientOrderId: 'karate-order-3' }
    When method POST
    Then status 400

  Scenario: No authentication returns 401
    Given path '/api/orders'
    And header X-API-Key = ''
    And request { clientOrderId: 'karate-order-4', symbol: 'AAPL', side: 'BUY', type: 'MARKET', quantity: 100, limitPrice: 0 }
    When method POST
    Then status 401
