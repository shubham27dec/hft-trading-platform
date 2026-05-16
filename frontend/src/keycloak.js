import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'hft',
  clientId: 'hft-frontend',
})

export default keycloak
