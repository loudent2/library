#!/bin/sh
set -e

echo "Waiting for DynamoDB Local to be ready..."

until aws dynamodb list-tables \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2 > /dev/null 2>&1; do
  echo "DynamoDB Local not ready yet..."
  sleep 2
done

echo "🧹 Deleting existing tables (if any)..."

TABLES=$(aws dynamodb list-tables \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2 \
  --query 'TableNames[]' \
  --output text)

for TABLE in $TABLES; do
  echo "  🔥 Deleting table: $TABLE"
  aws dynamodb delete-table \
    --table-name "$TABLE" \
    --endpoint-url http://dynamodb-local:8000 \
    --region us-west-2
done

# Wait for tables to be deleted (optional safety)
echo "⌛ Waiting for tables to be deleted..."
for TABLE in $TABLES; do
  until ! aws dynamodb describe-table \
    --table-name "$TABLE" \
    --endpoint-url http://dynamodb-local:8000 \
    --region us-west-2 > /dev/null 2>&1; do
    echo "  ⏳ Still deleting $TABLE..."
    sleep 2
  done
done

echo "✅ All previous tables deleted."

echo "Creating tables..."
echo "Creating Catalog table..."
aws dynamodb create-table \
  --table-name dev_Catalog \
  --attribute-definitions AttributeName=isbn,AttributeType=S \
  --key-schema AttributeName=isbn,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2

echo "populating Catalog table..."
aws dynamodb batch-write-item \
  --request-items file:///seed/catalog-seed.json \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2

echo "Creating Account table..."
aws dynamodb create-table \
  --table-name dev_Accounts \
  --attribute-definitions AttributeName=accountNumber,AttributeType=S \
  --key-schema AttributeName=accountNumber,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2

echo "Populating Account table..."
aws dynamodb batch-write-item \
  --request-items file:///seed/accounts-seed.json \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2

echo "Creating Activity table..."
aws dynamodb create-table \
  --table-name dev_Activity \
  --attribute-definitions \
    AttributeName=bookId,AttributeType=S \
    AttributeName=isbn,AttributeType=S \
    AttributeName=accountNumber,AttributeType=S \
  --key-schema \
    AttributeName=bookId,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "isbn-index",
      "KeySchema": [{"AttributeName":"isbn","KeyType":"HASH"}],
      "Projection": {"ProjectionType":"ALL"}
    },
    {
      "IndexName": "account-index",
      "KeySchema": [{"AttributeName":"accountNumber","KeyType":"HASH"}],
      "Projection": {"ProjectionType":"ALL"}
    }
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://dynamodb-local:8000 \
  --region us-west-2

# Optional seed data for Activity table (create /seed/activity-seed.json if needed). maybe laater
# echo "Populating Activity table..."
# aws dynamodb batch-write-item \
#   --request-items file:///seed/activity-seed.json \
#   --endpoint-url http://dynamodb-local:8000 \
#   --region us-west-2

echo "✅ DynamoDB setup complete."
