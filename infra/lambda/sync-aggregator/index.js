exports.handler = async (event) => {
    console.log(`Processing ${event.Records.length} DynamoDB Stream records...`);

    for (const record of event.Records) {
        try {
            if (record.eventName !== "INSERT") {
                console.log(`Skipping non-INSERT event: ${record.eventName}`);
                continue;
            }

            const image = record.dynamodb.NewImage;
            if (!image) continue;

            const item = {};
            for (const key in image) {
                const attr = image[key];
                // Handle standard S, N, BOOL unwraps for speed
                const val = attr.S || attr.N || attr.B || attr.BOOL;
                if (val !== undefined) item[key] = val;
            }

            console.log(`[AGGREGATOR] Tenant: ${item.tenantId} | Project: ${item.projectId} | Device: ${item.deviceId}`);
            
            if (item.payload) {
                const payloadObj = JSON.parse(item.payload);
                console.log(` -> Payload Data:`, payloadObj);
                // TODO: Sync-Relay or Roll-up Commit logic triggers here
            }

        } catch (err) {
            console.error("Error processing record:", err, record);
        }
    }

    return { status: "success", processed: event.Records.length };
};
