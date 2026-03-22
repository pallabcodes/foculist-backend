const { handler } = require('./index');

const mockEvent = {
    Records: [
        {
            eventID: "1001",
            eventName: "INSERT",
            dynamodb: {
                ApproximateCreationDateTime: 1724188656,
                Keys: {
                    pk: { S: "public#c69ffea3-a5da-443d-a039-7f44f79a720d" },
                    sk: { S: "2026-03-22T15:57:51.514Z#4714e338" }
                },
                NewImage: {
                    tenantId: { S: "public" },
                    projectId: { S: "c69ffea3-a5da-443d-a039-7f44f79a720d" },
                    deviceId: { S: "device-verify-123" },
                    destination: { S: "broadcast" },
                    payload: { S: '{"projectId":"c69ffea3-a5da-443d-a039-7f44f79a720e","testField":"Active_Dynamo_Data"}' },
                    occurredAt: { S: "2026-03-22T15:57:51.514872505Z" }
                }
            }
        }
    ]
};

console.log("=== INITIATING LOCAL LAMBDA RUN ===");
handler(mockEvent)
    .then(result => console.log("Result:", result))
    .catch(err => console.error("Error:", err));
