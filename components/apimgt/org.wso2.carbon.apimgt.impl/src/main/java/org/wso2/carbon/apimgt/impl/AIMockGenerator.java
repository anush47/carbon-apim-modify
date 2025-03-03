/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class represents the constants that are used for APIManager
 * implementation
 */
public final class AIMockGenerator {
        private static final String OPENAI_API_KEY = "";

        public JsonObject generateAIMockCode(String apiDefinition, String theme) {
                JsonObject swagger = cleanSwagger(apiDefinition);
                JsonObject bodyJson = bodySchema(swagger, theme);
                //JsonObject response = getModelResponse(OPENAI_API_KEY, bodyJson);
                JsonObject response = testResponse();
                System.out.println(response.get("mockDB").getAsString());
                return response;
        }

        private JsonObject responseFormatObject(JsonObject paths) {
                JsonObject responseFormatObject = new JsonObject();
                responseFormatObject.addProperty("type", "json_schema");
                JsonObject jsonSchemaObject = new JsonObject();
                jsonSchemaObject.addProperty("name", "mock-api");

                JsonObject pathsObject = new JsonObject();
                JsonObject pathsProperties = new JsonObject();
                JsonArray pathsRequired = new JsonArray();
                pathsObject.addProperty("type", "object");
                paths.asMap().forEach((path, methods) -> {
                        JsonObject pathObject = new JsonObject();
                        JsonObject pathProperties = new JsonObject();
                        JsonArray pathRequired = new JsonArray();
                        methods.getAsJsonObject().asMap().forEach((method, code) -> {
                                JsonObject methodObject = new JsonObject();
                                methodObject.addProperty("type", "object");
                                JsonObject methodProperties = new JsonObject();
                                JsonObject codeObject = new JsonObject();
                                codeObject.addProperty("type", "string");
                                methodProperties.add("code", codeObject);
                                methodObject.add("properties", methodProperties);
                                pathProperties.add(method, methodObject);
                                pathRequired.add(method);
                                JsonArray methodRequired = new JsonArray();
                                methodRequired.add("code");
                                methodObject.add("required", methodRequired);
                                methodObject.addProperty("additionalProperties", false);

                        });
                        pathObject.addProperty("type", "object");
                        pathObject.add("properties", pathProperties);
                        pathObject.add("required", pathRequired);
                        pathObject.addProperty("additionalProperties", false);
                        pathsProperties.add(path, pathObject);
                        pathsRequired.add(path);
                });
                pathsObject.add("properties", pathsProperties);
                pathsObject.add("required", pathsRequired);
                pathsObject.addProperty("additionalProperties", false);
                JsonObject mockDB = new JsonObject();
                mockDB.addProperty("type", "string");
                mockDB.addProperty("strict", true);
                JsonObject rootProperties = new JsonObject();
                rootProperties.add("mockDB", mockDB);
                rootProperties.add("paths", pathsObject);
                rootProperties.addProperty("additionalProperties", false);

                JsonObject schema = new JsonObject();
                schema.addProperty("type", "object");
                schema.add("properties", rootProperties);
                JsonArray rootRequired = new JsonArray();
                rootRequired.add("mockDB");
                rootRequired.add("paths");
                schema.add("required", rootRequired);
                schema.addProperty("additionalProperties", false);
                jsonSchemaObject.add("schema", schema);
                responseFormatObject.add("json_schema", jsonSchemaObject);

                return responseFormatObject;
        }

        private JsonObject bodySchema(JsonObject swagger, String theme) {
                String userMsg = generateOpenAPIMockPrompt(swagger, theme);
                String sysMsg = getSysMsg(swagger, theme);
                JsonObject userMsgObject = new JsonObject();
                userMsgObject.addProperty("role", "user");
                userMsgObject.addProperty("content", userMsg);
                JsonObject sysMsgObject = new JsonObject();
                sysMsgObject.addProperty("role", "system");
                sysMsgObject.addProperty("content", sysMsg);
                JsonArray messages = new JsonArray();
                messages.add(userMsgObject);
                messages.add(sysMsgObject);
                JsonObject responseFormatObject = responseFormatObject(swagger.get("paths").getAsJsonObject());

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("model", "gpt-4o-mini");
                bodyJson.add("messages", messages);
                bodyJson.add("response_format", responseFormatObject);

                return bodyJson;
        }

        private JsonObject getModelResponse(String apiKey, JsonObject body) {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + apiKey)
                                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                                .build();

                HttpClient client = HttpClient.newHttpClient();
                try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                        System.out.println(responseJson.toString());
                        JsonObject resultJson = JsonParser
                                        .parseString(responseJson.get("choices").getAsJsonArray().get(0)
                                                        .getAsJsonObject()
                                                        .get("message").getAsJsonObject().get("content").getAsString())
                                        .getAsJsonObject();
                        return resultJson;
                } catch (Exception e) {
                        e.printStackTrace();
                        JsonObject errObject = new JsonObject();
                        errObject.addProperty("error", e.getMessage());
                        return errObject;
                }
        }

        private static JsonObject testResponse() {
                String response = "{\"mockDB\":\"{\\\"orders\\\":[{\\\"orderId\\\":\\\"1\\\",\\\"customerName\\\":\\\"John Doe\\\",\\\"pizzaType\\\":\\\"Veggie\\\",\\\"quantity\\\":2,\\\"address\\\":\\\"123 Main St, Colombo\\\",\\\"creditCardNumber\\\":\\\"1234-5678-9876-5432\\\",\\\"delivered\\\":false},{\\\"orderId\\\":\\\"2\\\",\\\"customerName\\\":\\\"Jane Smith\\\",\\\"pizzaType\\\":\\\"Pepperoni\\\",\\\"quantity\\\":1,\\\"address\\\":\\\"456 Elm St, Colombo\\\",\\\"creditCardNumber\\\":\\\"2345-6789-8765-4321\\\",\\\"delivered\\\":true},{\\\"orderId\\\":\\\"3\\\",\\\"customerName\\\":\\\"Rohan Perera\\\",\\\"pizzaType\\\":\\\"Margherita\\\",\\\"quantity\\\":3,\\\"address\\\":\\\"789 Oak St, Colombo\\\",\\\"creditCardNumber\\\":\\\"3456-7890-7654-3210\\\",\\\"delivered\\\":false}]}\",\"paths\":{\"/order\":{\"post\":{\"code\":\"var accept = mc.getProperty('AcceptHeader');\\nif (accept == null) { accept = 'application/json'; }\\nif (accept == 'application/json') {\\n    mc.setProperty('CONTENT_TYPE', 'application/json');\\n    var db = JSON.parse(mc.getProperty('mockDB') || '{\\\"orders\\\":[]}');\\n    var body = mc.getPayloadJSON();\\n    var requiredFields = ['customerName', 'pizzaType', 'quantity', 'address', 'creditCardNumber'];\\n    var valid = true;\\n    for (var i = 0; i < requiredFields.length; i++) {\\n        if (!body[requiredFields[i]]) {\\n            valid = false;\\n            break;\\n        }\\n    }\\n    if (valid) {\\n        body.orderId = (db.orders.length + 1).toString();\\n        db.orders.push(body);\\n        mc.setProperty('HTTP_SC', '201');\\n        mc.setProperty('Location', '/order/' + body.orderId);\\n        mc.setPayloadJSON(body);\\n    } else {\\n        mc.setProperty('HTTP_SC', '400');\\n        mc.setPayloadJSON({message: 'Invalid request. Missing required fields.'});\\n    }\\n    mc.setProperty('mockDB', JSON.stringify(db));\\n}\"}},\"/menu\":{\"get\":{\"code\":\"var accept = mc.getProperty('AcceptHeader');\\nif (accept == null) { accept = 'application/json'; }\\nif (accept == 'application/json') {\\n    mc.setProperty('CONTENT_TYPE', 'application/json');\\n    var menuItems = [{\\\"name\\\":\\\"Veggie Pizza\\\",\\\"price\\\":\\\"9.99\\\",\\\"description\\\":\\\"Delicious veggie pizza\\\",\\\"image\\\":\\\"/images/veggie.png\\\"},{\\\"name\\\":\\\"Pepperoni Pizza\\\",\\\"price\\\":\\\"11.99\\\",\\\"description\\\":\\\"Spicy pepperoni pizza\\\",\\\"image\\\":\\\"/images/pepperoni.png\\\"},{\\\"name\\\":\\\"Margherita Pizza\\\",\\\"price\\\":\\\"10.49\\\",\\\"description\\\":\\\"Classic Margherita pizza\\\",\\\"image\\\":\\\"/images/margherita.png\\\"}];\\n    mc.setPayloadJSON(menuItems);\\n    mc.setProperty('HTTP_SC', '200');\\n}\"}},\"/order/{orderId}\":{\"get\":{\"code\":\"var accept = mc.getProperty('AcceptHeader');\\nif (accept == null) { accept = 'application/json'; }\\nif (accept == 'application/json') {\\n    mc.setProperty('CONTENT_TYPE', 'application/json');\\n    var id = mc.getProperty('REST_FULL_REQUEST_PATH').split('/').pop();\\n    var db = JSON.parse(mc.getProperty('mockDB') || '{\\\"orders\\\":[]}');\\n    var order = null;\\n    for (var i = 0; i < (db.orders || []).length; i++) {\\n        if (db.orders[i].orderId == id) {\\n            order = db.orders[i];\\n            break;\\n        }\\n    }\\n    mc.setPayloadJSON(order || {message: 'Order not found'});\\n    mc.setProperty('HTTP_SC', order ? '200' : '404');\\n}\"},\"put\":{\"code\":\"var accept = mc.getProperty('AcceptHeader');\\nif (accept == null) { accept = 'application/json'; }\\nif (accept == 'application/json') {\\n    mc.setProperty('CONTENT_TYPE', 'application/json');\\n    var id = mc.getProperty('REST_FULL_REQUEST_PATH').split('/').pop();\\n    var db = JSON.parse(mc.getProperty('mockDB') || '{\\\"orders\\\":[]}');\\n    var body = mc.getPayloadJSON();\\n    var requiredFields = ['customerName', 'pizzaType', 'quantity', 'address', 'creditCardNumber'];\\n    var valid = true;\\n    for (var i = 0; i < requiredFields.length; i++) {\\n        if (!body[requiredFields[i]]) {\\n            valid = false;\\n            break;\\n        }\\n    }\\n    if (valid) {\\n        for (var i = 0; i < db.orders.length; i++) {\\n            if (db.orders[i].orderId == id) {\\n                db.orders[i] = body;\\n                db.orders[i].orderId = id;\\n                mc.setProperty('HTTP_SC', '200');\\n                mc.setPayloadJSON(body);\\n                mc.setProperty('mockDB', JSON.stringify(db));\\n                return;\\n            }\\n        }\\n        mc.setProperty('HTTP_SC', '404');\\n        mc.setPayloadJSON({message: 'Order not found'});\\n    } else {\\n        mc.setProperty('HTTP_SC', '400');\\n        mc.setPayloadJSON({message: 'Invalid request. Missing required fields.'});\\n    }\\n}\"},\"delete\":{\"code\":\"var accept = mc.getProperty('AcceptHeader');\\nif (accept == null) { accept = 'application/json'; }\\nif (accept == 'application/json') {\\n    mc.setProperty('CONTENT_TYPE', 'application/json');\\n    var id = mc.getProperty('REST_FULL_REQUEST_PATH').split('/').pop();\\n    var db = JSON.parse(mc.getProperty('mockDB') || '{\\\"orders\\\":[]}');\\n    var found = false;\\n    for (var i = 0; i < db.orders.length; i++) {\\n        if (db.orders[i].orderId == id) {\\n            db.orders.splice(i, 1);\\n            found = true;\\n            mc.setProperty('HTTP_SC', '200');\\n            break;\\n        }\\n    }\\n    if (!found) {\\n        mc.setProperty('HTTP_SC', '404');\\n        mc.setPayloadJSON({message: 'Order not found'});\\n    } else {\\n        mc.setProperty('mockDB', JSON.stringify(db));\\n    }\\n}\"}}}}";
                JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
                // JsonObject resultJson = JsonParser
                //                 .parseString(responseJson.get("choices").getAsJsonArray().get(0).getAsJsonObject()
                //                                 .get("message").getAsJsonObject().get("content").getAsString())
                //                 .getAsJsonObject();
                return responseJson;
        }

        private String generateOpenAPIMockPrompt(JsonObject swagger, String theme) {
                return "Generate ES3 JavaScript for rhinojs to mock OpenAPI from: " + swagger.toString() + "\n\n"
                                + "Instructions:\n"
                                + "- Extract request data via mc.getProperty() and mc.getPayloadJSON().\n"
                                + "- Get path params in the format mc.getProperty('uri.var.petId'). petId is the parameter name. \n"
                                + "- Prepopulate mockDB with 3+ '" + theme + "' themed records.\n"
                                + "- Load mockDB from mc.getProperty('mockDB').\n"
                                + "- Handle all status codes, support JSON/XML, and persist mockDB updates "
                                + "by setting the mockDB if changed using mc.setProperty('mockDB', JSON.stringify(db)).\n"
                                + "- Use loops only (no find, filter, map, reduce, or spread syntax like {...orders, ...requestBody}).\n"
                                + "- Always when looping through an array, check if the item exists before accessing it. Make sure no run time errors occur\n"
                                + "- Use strict mode and validate all requests.\n"
                                + "- Validate payloads against schema, check required fields, and handle errors.\n"
                                + "- Assign responses using mc.setProperty() and mc.setPayloadJSON().\n"
                                + "- Ensure mock server behaves like a real one.\n\n"
                                + "Expected Output Example:\n"
                                + "{ \"mockDB\": \"{\\\"pets\\\":[{\\\"id\\\":1,\\\"name\\\":\\\"Whiskers\\\"}]}\", \"paths\": {\n"
                                + "  \"/pets\": {\n"
                                + "    \"get\": {\"code\": \""
                                + "var accept = mc.getProperty('AcceptHeader');\n"
                                + "if (accept == null) { accept = 'application/json'; }\n"
                                + "if (accept == 'application/json') {\n"
                                + "  mc.setProperty('CONTENT_TYPE', 'application/json');\n"
                                + "  var db = JSON.parse(mc.getProperty('mockDB') || '{}');\n"
                                + "  mc.setPayloadJSON(db.pets || []);\n"
                                + "  mc.setProperty('HTTP_SC', '200');\n"
                                + "}\n"
                                + "\"},\n"
                                + "    \"post\": {\"code\": \""
                                + "var accept = mc.getProperty('AcceptHeader');\n"
                                + "if (accept == null) { accept = 'application/json'; }\n"
                                + "if (accept == 'application/json') {\n"
                                + "  mc.setProperty('CONTENT_TYPE', 'application/json');\n"
                                + "  var db = JSON.parse(mc.getProperty('mockDB') || '{}');\n"
                                + "  var body = mc.getPayloadJSON();\n"
                                + "  var valid = body && body.id && body.name;\n"
                                + "  if (valid) {\n"
                                + "    db.pets = db.pets || [];\n"
                                + "    db.pets.push(body);\n"
                                + "    mc.setProperty('HTTP_SC', '201');\n"
                                + "  } else {\n"
                                + "    mc.setProperty('HTTP_SC', '400');\n"
                                + "    mc.setPayloadJSON({message: 'Invalid request'});\n"
                                + "  }\n"
                                + "  mc.setProperty('mockDB', JSON.stringify(db));\n"
                                + "}\n"
                                + "\"}\n"
                                + "  },\n"
                                + "  \"/pets/{petId}\": {\n"
                                + "    \"get\": {\"code\": \""
                                + "var accept = mc.getProperty('AcceptHeader');\n"
                                + "if (accept == null) { accept = 'application/json'; }\n"
                                + "if (accept == 'application/json') {\n"
                                + "  mc.setProperty('CONTENT_TYPE', 'application/json');\n"
                                + "  var id = mc.getProperty('REST_FULL_REQUEST_PATH').split('/').pop();\n"
                                + "  var db = JSON.parse(mc.getProperty('mockDB') || '{}');\n"
                                + "  var pet = null;\n"
                                + "  for (var i = 0; i < (db.pets || []).length; i++) {\n"
                                + "    if (db.pets[i].id == id) {\n"
                                + "      pet = db.pets[i];\n"
                                + "      break;\n"
                                + "    }\n"
                                + "  }\n"
                                + "  mc.setPayloadJSON(pet || {message: 'Pet not found'});\n"
                                + "  mc.setProperty('HTTP_SC', pet ? '200' : '404');\n"
                                + "}\n"
                                + "\"}\n"
                                + "  }\n"
                                + "} }";
        }

        private String getSysMsg(JsonObject swagger, String theme) {
                return "Design a scalable, secure, and well-documented mock API for testing, ensuring ease of use, maintenance, and no sensitive data exposure.";
        }

        private JsonObject cleanSwagger(String apiDefinition) {
                JsonObject swaggerJson = JsonParser.parseString(apiDefinition).getAsJsonObject();

                JsonObject paths = swaggerJson.getAsJsonObject("paths");
                paths.entrySet().forEach(path -> {
                        path.getValue().getAsJsonObject().entrySet().forEach(method -> {
                                method.getValue().getAsJsonObject().remove("security");
                                method.getValue().getAsJsonObject().remove("x-auth-type");
                                method.getValue().getAsJsonObject().remove("x-throttling-tier");
                                method.getValue().getAsJsonObject().remove("x-mediation-script");
                                method.getValue().getAsJsonObject().remove("x-wso2-application-security");
                                method.getValue().getAsJsonObject().remove("externalDocs");
                                method.getValue().getAsJsonObject().remove("parameters");
                        });
                });
                JsonObject components = swaggerJson.getAsJsonObject("components");
                components.remove("securitySchemes");
                JsonObject cleanedSwagger = new JsonObject();
                cleanedSwagger.add("paths", paths);
                cleanedSwagger.add("components", components);
                return cleanedSwagger;
        }

}
