#!/bin/bash
# API Test Script for ABA-OS
# Runs all api-test.http endpoints in sequence

set -e

BASE_URL="http://localhost:8080/api/v1"

echo "=== Starting API Tests ==="
echo ""

# Step 1: Admin Login
echo "=== Step 1-2: Admin Login ==="
ADMIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"admin1234"}')
echo $ADMIN_RESPONSE

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Failed to get admin token"
  exit 1
fi
echo "Admin token obtained"
echo ""

# Step 2: Therapist Login
echo "=== Step 2-2: Therapist Login ==="
THERAPIST_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"therapist@gmail.com","password":"therapist1234"}')
echo $THERAPIST_RESPONSE

THERAPIST_TOKEN=$(echo $THERAPIST_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$THERAPIST_TOKEN" ]; then
  echo "ERROR: Failed to get therapist token"
  exit 1
fi
echo "Therapist token obtained"
echo ""

# Step 2-3: Get Therapist User ID
echo "=== Step 2-3: Get Therapist User Info ==="
USER_INFO=$(curl -s -X GET "$BASE_URL/users/me" \
  -H "Authorization: Bearer $THERAPIST_TOKEN")
echo $USER_INFO
THERAPIST_USER_ID=$(echo $USER_INFO | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Therapist User ID: $THERAPIST_USER_ID"
echo ""

# Step 2-4: Get Therapist by User ID
echo "=== Step 2-4: Get Therapist by User ID ==="
THERAPIST_INFO=$(curl -s -X GET "$BASE_URL/therapists/by-user/$THERAPIST_USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
echo $THERAPIST_INFO
THERAPIST_ID=$(echo $THERAPIST_INFO | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Therapist ID: $THERAPIST_ID"
echo ""

# Step 2-5: Update Therapist Info (using ASCII text)
echo "=== Step 2-5: Update Therapist Info ==="
UPDATE_RESULT=$(curl -s -X PUT "$BASE_URL/therapists/$THERAPIST_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"specialty":"ABA Therapy","experienceYears":3}')
echo $UPDATE_RESULT
echo ""

# Step 2-6: Get Therapist List
echo "=== Step 2-6: Therapist List ==="
curl -s -X GET "$BASE_URL/therapists" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
echo ""
echo ""

# Step 3-1: Check if child exists
echo "=== Step 3: Child Registration ==="
CHILDREN_LIST=$(curl -s -X GET "$BASE_URL/children" \
  -H "Authorization: Bearer $THERAPIST_TOKEN")
echo "Current children list: $CHILDREN_LIST"

CHILD_ID=$(echo $CHILDREN_LIST | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$CHILD_ID" ]; then
  echo "No children found, creating new child..."
  CHILD_RESULT=$(curl -s -X POST "$BASE_URL/children" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{\"name\":\"Test Child\",\"birthDate\":\"2020-03-15\",\"gender\":\"MALE\",\"diagnosis\":\"ASD\",\"currentDevLevel\":\"Delayed\",\"parentCharacteristics\":\"Supportive\",\"requestDetails\":\"Eye contact improvement\",\"therapistId\":$THERAPIST_ID}")
  echo $CHILD_RESULT
  CHILD_ID=$(echo $CHILD_RESULT | grep -o '"data":[0-9]*' | cut -d':' -f2)
fi
echo "Child ID: $CHILD_ID"
echo ""

# Step 4-2: Get Child List
echo "=== Step 4-2: Child List ==="
curl -s -X GET "$BASE_URL/children" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 4-3: Get Child Detail
echo "=== Step 4-3: Child Detail ==="
curl -s -X GET "$BASE_URL/children/$CHILD_ID" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 5-1: Create Goal (check if exists first)
echo "=== Step 5: Goal Management ==="
GOALS_LIST=$(curl -s -X GET "$BASE_URL/children/$CHILD_ID/goals" \
  -H "Authorization: Bearer $THERAPIST_TOKEN")
echo "Current goals: $GOALS_LIST"

GOAL_ID=$(echo $GOALS_LIST | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$GOAL_ID" ]; then
  echo "No goals found, creating new goal..."
  GOAL_RESULT=$(curl -s -X POST "$BASE_URL/children/$CHILD_ID/goals" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $THERAPIST_TOKEN" \
    -d '{"name":"Eye Contact Training","category":"SOCIAL","description":"Make eye contact within 3 seconds when name is called","targetSuccessRate":80,"consecutiveDays":3,"promptPlan":"Physical -> Gesture -> Verbal -> Independent"}')
  echo $GOAL_RESULT
  GOAL_ID=$(echo $GOAL_RESULT | grep -o '"data":[0-9]*' | cut -d':' -f2)
fi
echo "Goal ID: $GOAL_ID"
echo ""

# Step 5-2: Get Goals List
echo "=== Step 5-2: Goals List ==="
curl -s -X GET "$BASE_URL/children/$CHILD_ID/goals" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 5-3: Get Goal Detail
echo "=== Step 5-3: Goal Detail ==="
curl -s -X GET "$BASE_URL/goals/$GOAL_ID" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 6-1: Create Session
echo "=== Step 6-1: Create Session ==="
SESSION_RESULT=$(curl -s -X POST "$BASE_URL/sessions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $THERAPIST_TOKEN" \
  -d "{\"childId\":$CHILD_ID,\"therapistId\":$THERAPIST_ID,\"sessionDate\":\"2026-02-07\",\"duration\":50,\"notes\":\"First session. Good condition.\",\"trials\":[{\"goalId\":$GOAL_ID,\"taskContent\":\"Eye contact training\",\"trials\":10,\"successes\":6,\"promptCount\":3,\"memo\":\"Good response to verbal prompts\"}]}")
echo $SESSION_RESULT
SESSION_ID=$(echo $SESSION_RESULT | grep -o '"data":[0-9]*' | cut -d':' -f2)
echo "Session ID: $SESSION_ID"
echo ""

# Step 6-2: Create Additional Session
echo "=== Step 6-2: Create Additional Session ==="
curl -s -X POST "$BASE_URL/sessions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $THERAPIST_TOKEN" \
  -d "{\"childId\":$CHILD_ID,\"therapistId\":$THERAPIST_ID,\"sessionDate\":\"2026-02-05\",\"duration\":50,\"notes\":\"Second session. Improvement observed.\",\"trials\":[{\"goalId\":$GOAL_ID,\"taskContent\":\"Eye contact training\",\"trials\":12,\"successes\":9,\"promptCount\":2,\"memo\":\"Transitioning to gesture prompts\"}]}"
echo ""
echo ""

# Step 6-3: Get Sessions List
echo "=== Step 6-3: Sessions List ==="
curl -s -X GET "$BASE_URL/sessions?childId=$CHILD_ID" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 6-4: Get Session Detail
echo "=== Step 6-4: Session Detail ==="
if [ ! -z "$SESSION_ID" ]; then
  curl -s -X GET "$BASE_URL/sessions/$SESSION_ID" \
    -H "Authorization: Bearer $THERAPIST_TOKEN"
fi
echo ""
echo ""

# Step 7-1: Create AI Report
echo "=== Step 7-1: Create AI Report (PARENT_SUMMARY) ==="
REPORT_RESULT=$(curl -s -X POST "$BASE_URL/reports" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $THERAPIST_TOKEN" \
  -d "{\"childId\":$CHILD_ID,\"title\":\"February 2026 Development Report (AI)\",\"periodStart\":\"2026-02-01\",\"periodEnd\":\"2026-02-28\",\"reportType\":\"PARENT_SUMMARY\"}")
echo $REPORT_RESULT
REPORT_ID=$(echo $REPORT_RESULT | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Report ID: $REPORT_ID"
echo ""

# Step 7-2: Create Statistics Report
echo "=== Step 7-2: Create Statistics Report ==="
curl -s -X POST "$BASE_URL/reports" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $THERAPIST_TOKEN" \
  -d "{\"childId\":$CHILD_ID,\"title\":\"February 2026 Statistics Report\",\"periodStart\":\"2026-02-01\",\"periodEnd\":\"2026-02-28\",\"reportType\":\"STATISTICS_ONLY\"}"
echo ""
echo ""

# Step 7-3: Get Reports List
echo "=== Step 7-3: Reports List ==="
curl -s -X GET "$BASE_URL/reports?childId=$CHILD_ID" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

# Step 7-4: Get Report Detail
echo "=== Step 7-4: Report Detail ==="
if [ ! -z "$REPORT_ID" ]; then
  curl -s -X GET "$BASE_URL/reports/$REPORT_ID" \
    -H "Authorization: Bearer $THERAPIST_TOKEN"
fi
echo ""
echo ""

# Step 8-1: Dashboard (Admin)
echo "=== Step 8-1: Dashboard Summary (Admin) ==="
curl -s -X GET "$BASE_URL/dashboard/summary" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
echo ""
echo ""

# Step 8-2: Dashboard (Therapist)
echo "=== Step 8-2: Dashboard Summary (Therapist) ==="
curl -s -X GET "$BASE_URL/dashboard/summary" \
  -H "Authorization: Bearer $THERAPIST_TOKEN"
echo ""
echo ""

echo ""
echo "=== All Tests Completed Successfully! ==="
