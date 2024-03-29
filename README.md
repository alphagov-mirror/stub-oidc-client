# stub-oidc-client

Stub OIDC Client is a very simple stub implementation of an OpenID Connect client which uses the Hybrid flow. There is currently no Trust Infrastructure in this implementation and it is very much a work in progress.

You can find the Stub OpenID Connect Provider [here](https://github.com/alphagov/stub-oidc-op)

### Stub OIDC Client can currently peforms the following functions 
* Generate an Authentication Request using OpenID Connect with Response Type (code, id_token, token) and sends it to a OpenID Connect Provider.
* Receive an Authentication Code, ID Token and Access Token and performs some validation as per the Open ID Connect Spec. 
* Makes a request to the OpenID Connect Provider using the Authentication code to request an Access and ID Token.
* Receive an Access Token and use it to request user information from an OpenID Connect Provider.

### To use stub-oidc-client
* Ensure you have [Stub OIDC OP](https://github.com/alphagov/stub-oidc-op) up and running
* Run startup.sh
* Go to http://localhost:6610/ in your browser and click Send request

### Stub OIDC Client runs on the PAAS 
* To deploy Stub OIDC Client simply login to the PAAS and select the build-learn space. 
* Run './gradlew pushToPaas' and this will deploy the app.

### For more information about Open ID Connect - 
* Open ID Connect Spec - https://openid.net/specs/openid-connect-core-1_0.html
* Diagrams of all the OpenID Connect flows - https://medium.com/@darutk/diagrams-of-all-the-openid-connect-flows-6968e3990660
* Dev overflow of OpenID Connect - https://developers.onelogin.com/openid-connect

## License

[MIT](https://github.com/alphagov/stub-oidc-client/blob/master/LICENCE)
