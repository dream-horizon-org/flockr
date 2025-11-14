# Sample API Responses

## 1. Complete Audience Details

**Request:**
```http
GET /api/v1/audiences/123/details
```

**Response:**
```json
{
  "success": true,
  "data": {
    "audienceId": 123,
    "tenantId": "tenant-001",
    "projectId": "project-abc",
    "name": "High Value Customers - Q4 2024",
    "description": "Customers with lifetime value > $10,000 or purchase frequency > 10 per month",
    "type": "DYNAMIC",
    "customAudienceConfig": {
      "refreshInterval": 3600,
      "includeAnonymous": false,
      "timezone": "UTC"
    },
    "verified": true,
    "userCount": 15420,
    "rulesCount": 3,
    "expireDate": 1735689600000,
    "lastAudienceUpdatedAt": 1699900000000,
    "createdAt": 1699800000000,
    "updatedAt": 1699900000000,
    "createdBy": "admin@example.com",
    
    "sinks": [
      {
        "id": 10,
        "name": "Facebook Custom Audience",
        "typeId": 1,
        "type": "facebook",
        "config": {
          "accountId": "act_1234567890",
          "accessToken": "***",
          "customAudienceId": "123456789012345"
        },
        "status": "ACTIVE",
        "createdBy": "marketing@example.com"
      },
      {
        "id": 11,
        "name": "Google Ads Customer Match",
        "typeId": 2,
        "type": "google_ads",
        "config": {
          "customerId": "123-456-7890",
          "userListId": "987654321"
        },
        "status": "ACTIVE",
        "createdBy": "marketing@example.com"
      },
      {
        "id": 12,
        "name": "Braze User Segment",
        "typeId": 5,
        "type": "braze",
        "config": {
          "apiKey": "***",
          "appGroupId": "app-group-xyz",
          "segmentId": "seg-12345"
        },
        "status": "ACTIVE",
        "createdBy": "product@example.com"
      }
    ],
    
    "rules": [
      {
        "ruleId": 1001,
        "name": "Daily Batch - High LTV Users",
        "description": "Import users with high lifetime value from data warehouse",
        "startTime": null,
        "endTime": null,
        "ruleAction": "ADD",
        "ruleType": "BATCH",
        "status": "ACTIVE",
        "configuration": {
          "type": "BATCH",
          "cronExpression": "0 0 2 * * ?",
          "sourceId": 101,
          "query": "SELECT DISTINCT user_id FROM analytics.user_metrics WHERE lifetime_value > 10000 AND last_purchase_date > DATE_SUB(NOW(), INTERVAL 90 DAY)"
        },
        "createdBy": "data-eng@example.com",
        "updatedBy": null,
        "createdAt": 1699800000000,
        "updatedAt": 1699800000000,
        "sources": [
          {
            "sourceId": 101,
            "name": "Snowflake Data Warehouse",
            "type": "snowflake",
            "status": "ACTIVE",
            "createdBy": "data-eng@example.com",
            "query": "SELECT DISTINCT user_id FROM analytics.user_metrics WHERE lifetime_value > 10000 AND last_purchase_date > DATE_SUB(NOW(), INTERVAL 90 DAY)",
            "eventName": null
          }
        ]
      },
      {
        "ruleId": 1002,
        "name": "Stream - High Frequency Purchasers",
        "description": "Real-time detection of users making 3+ purchases within 30 days",
        "startTime": 1699800000000,
        "endTime": null,
        "ruleAction": "ADD",
        "ruleType": "STREAM",
        "status": "ACTIVE",
        "configuration": {
          "type": "STREAM",
          "pattern": {
            "groupBy": ["user_id"],
            "pattern": [
              {
                "order": 1,
                "contiguity": "STRICT",
                "data": {
                  "quantifier": {
                    "conditionOperator": ">=",
                    "conditionValue": 3
                  },
                  "event": [
                    {
                      "sourceId": 201,
                      "eventName": "purchase_completed",
                      "condition": [
                        {
                          "filterType": "event",
                          "propertyName": "amount",
                          "propertyType": "number",
                          "conditionOperator": ">",
                          "conditionValue": "100"
                        },
                        {
                          "filterType": "event",
                          "propertyName": "status",
                          "propertyType": "string",
                          "conditionOperator": "=",
                          "conditionValue": "confirmed"
                        }
                      ]
                    }
                  ]
                }
              }
            ],
            "constraint": {
              "temporal": "within",
              "timeUnit": "DAYS",
              "value": 30
            }
          }
        },
        "createdBy": "data-eng@example.com",
        "updatedBy": "data-eng@example.com",
        "createdAt": 1699810000000,
        "updatedAt": 1699820000000,
        "sources": [
          {
            "sourceId": 201,
            "name": "Kafka Purchase Events",
            "type": "kafka",
            "status": "ACTIVE",
            "createdBy": "data-eng@example.com",
            "query": null,
            "eventName": "purchase_completed"
          }
        ]
      },
      {
        "ruleId": 1003,
        "name": "Stream - Product Category Sequence",
        "description": "Users who viewed category A, then added category B item to cart, then purchased",
        "startTime": 1699800000000,
        "endTime": null,
        "ruleAction": "ADD",
        "ruleType": "STREAM",
        "status": "ACTIVE",
        "configuration": {
          "type": "STREAM",
          "pattern": {
            "groupBy": ["user_id"],
            "pattern": [
              {
                "order": 1,
                "contiguity": "RELAXED",
                "data": {
                  "quantifier": {
                    "conditionOperator": "=",
                    "conditionValue": 1
                  },
                  "event": [
                    {
                      "sourceId": 202,
                      "eventName": "category_viewed",
                      "condition": [
                        {
                          "filterType": "event",
                          "propertyName": "category",
                          "propertyType": "string",
                          "conditionOperator": "=",
                          "conditionValue": "electronics"
                        }
                      ]
                    }
                  ]
                }
              },
              {
                "order": 2,
                "contiguity": "RELAXED",
                "data": {
                  "quantifier": {
                    "conditionOperator": "=",
                    "conditionValue": 1
                  },
                  "event": [
                    {
                      "sourceId": 202,
                      "eventName": "add_to_cart",
                      "condition": [
                        {
                          "filterType": "event",
                          "propertyName": "category",
                          "propertyType": "string",
                          "conditionOperator": "=",
                          "conditionValue": "home_appliances"
                        }
                      ]
                    }
                  ]
                }
              },
              {
                "order": 3,
                "contiguity": "STRICT",
                "data": {
                  "quantifier": {
                    "conditionOperator": "=",
                    "conditionValue": 1
                  },
                  "event": [
                    {
                      "sourceId": 201,
                      "eventName": "purchase_completed",
                      "condition": []
                    }
                  ]
                }
              }
            ],
            "constraint": {
              "temporal": "within",
              "timeUnit": "HOURS",
              "value": 24
            },
            "cohortFilter": {
              "belongsTo": [],
              "notBelongsTo": ["audience_456", "audience_789"]
            }
          }
        },
        "createdBy": "product@example.com",
        "updatedBy": null,
        "createdAt": 1699850000000,
        "updatedAt": 1699850000000,
        "sources": [
          {
            "sourceId": 202,
            "name": "Website Clickstream Events",
            "type": "kafka",
            "status": "ACTIVE",
            "createdBy": "data-eng@example.com",
            "query": null,
            "eventName": "category_viewed"
          },
          {
            "sourceId": 202,
            "name": "Website Clickstream Events",
            "type": "kafka",
            "status": "ACTIVE",
            "createdBy": "data-eng@example.com",
            "query": null,
            "eventName": "add_to_cart"
          },
          {
            "sourceId": 201,
            "name": "Kafka Purchase Events",
            "type": "kafka",
            "status": "ACTIVE",
            "createdBy": "data-eng@example.com",
            "query": null,
            "eventName": "purchase_completed"
          }
        ]
      }
    ]
  }
}
```

## 2. Audience Summary

**Request:**
```http
GET /api/v1/audiences/123/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "audienceId": 123,
    "name": "High Value Customers - Q4 2024",
    "description": "Customers with lifetime value > $10,000 or purchase frequency > 10 per month",
    "type": "DYNAMIC",
    "userCount": 15420,
    "sinksCount": 3,
    "rulesCount": 3,
    "verified": true,
    "createdAt": 1699800000000,
    "updatedAt": 1699900000000
  }
}
```

## 3. Rules with Sources Only

**Request:**
```http
GET /api/v1/audiences/123/rules-with-sources
```

**Response:**
```json
{
  "success": true,
  "data": {
    "audienceId": 123,
    "audienceName": "High Value Customers - Q4 2024",
    "rules": [
      {
        "ruleId": 1001,
        "name": "Daily Batch - High LTV Users",
        "ruleType": "BATCH",
        "status": "ACTIVE",
        "sources": [
          {
            "sourceId": 101,
            "name": "Snowflake Data Warehouse",
            "type": "snowflake",
            "status": "ACTIVE",
            "query": "SELECT DISTINCT user_id FROM analytics.user_metrics WHERE lifetime_value > 10000"
          }
        ]
      },
      {
        "ruleId": 1002,
        "name": "Stream - High Frequency Purchasers",
        "ruleType": "STREAM",
        "status": "ACTIVE",
        "sources": [
          {
            "sourceId": 201,
            "name": "Kafka Purchase Events",
            "type": "kafka",
            "status": "ACTIVE",
            "eventName": "purchase_completed"
          }
        ]
      },
      {
        "ruleId": 1003,
        "name": "Stream - Product Category Sequence",
        "ruleType": "STREAM",
        "status": "ACTIVE",
        "sources": [
          {
            "sourceId": 202,
            "name": "Website Clickstream Events",
            "type": "kafka",
            "status": "ACTIVE",
            "eventName": "category_viewed"
          },
          {
            "sourceId": 202,
            "name": "Website Clickstream Events",
            "type": "kafka",
            "status": "ACTIVE",
            "eventName": "add_to_cart"
          },
          {
            "sourceId": 201,
            "name": "Kafka Purchase Events",
            "type": "kafka",
            "status": "ACTIVE",
            "eventName": "purchase_completed"
          }
        ]
      }
    ]
  }
}
```

## 4. Error Response

**Request:**
```http
GET /api/v1/audiences/99999/details
```

**Response (404):**
```json
{
  "success": false,
  "error": {
    "code": 404,
    "type": "NOT_FOUND",
    "message": "Audience with ID 99999 not found"
  }
}
```

**Response (500):**
```json
{
  "success": false,
  "error": {
    "code": 500,
    "type": "INTERNAL_ERROR",
    "message": "Failed to fetch audience details: Database connection timeout"
  }
}
```

## Notes on Response Structure

### Source Metadata Context
- **BATCH rules**: Sources include `query` field with the SQL query
- **STREAM rules**: Sources include `eventName` field with the specific event
- Same source can appear multiple times with different event names in STREAM rules

### Timestamps
- All timestamps are in Unix epoch milliseconds (Long)
- Null timestamps indicate no value set (e.g., rules with no end time)

### Configuration Field
- Contains the complete rule configuration as JSON
- Structure varies by rule type (BATCH vs STREAM)
- Preserved as-is from database for flexibility

### Status Values
- Audience: No status field (always active or deleted)
- Sinks: `ACTIVE`, `INACTIVE`, `ERROR`
- Rules: `ACTIVE`, `INACTIVE`, `DRAFT`, `ERROR`
- Sources: `ACTIVE`, `INACTIVE`, `ERROR`

