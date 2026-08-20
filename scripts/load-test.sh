#!/bin/bash
# 博客 + 股票核心接口压测
# 用法: ./load-test.sh [并发数] [总请求数]

HOST="${HOST:-https://www.3ks2u.com}"
CONCURRENCIES="${1:-10 50 100}"
TOTAL="${2:-1000}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_DIR="./load-test-results/$TIMESTAMP"
mkdir -p "$RESULT_DIR"

# 索引数组: name1 path1 name2 path2 ...
ENDPOINTS=(
  "blog-list"     "/api/blog/list?page=1&pageSize=10"
  "blog-detail"   "/api/blog/82"
  "stock-indices" "/api/stock/v2/indices"
  "stock-market"  "/api/stock/v2/market"
  "health"        "/health"
)

run_test() {
  local name="$1"
  local path="$2"
  local c="$3"
  local n="$4"
  local out="$RESULT_DIR/${name}_c${c}.txt"

  echo ">>> $name  |  concurrency=$c  |  requests=$n"
  ab -q -k -c "$c" -n "$n" -H "Accept-Encoding: gzip" "$HOST$path" > "$out" 2>&1

  local qps t50 t95 t99 fail ok
  qps=$(grep "Requests per second" "$out" | awk '{print $4}')
  t50=$(grep "50%" "$out" | awk '{print $2}')
  t95=$(grep "95%" "$out" | awk '{print $2}')
  t99=$(grep "99%" "$out" | awk '{print $2}')
  fail=$(grep "Failed requests" "$out" | awk '{print $3}')

  printf "     QPS: %-10s  p50: %-8s  p95: %-8s  p99: %-8s  failed: %s\n" \
    "$qps" "${t50}ms" "${t95}ms" "${t99}ms" "$fail"
  echo ""
}

echo "========================================="
echo "  Target: $HOST"
echo "  Concurrency: $CONCURRENCIES"
echo "  Requests/round: $TOTAL"
echo "  Results: $RESULT_DIR"
echo "========================================="
echo ""

for c in $CONCURRENCIES; do
  echo "--- concurrency = $c ---"
  for ((i=0; i<${#ENDPOINTS[@]}; i+=2)); do
    name="${ENDPOINTS[$i]}"
    path="${ENDPOINTS[$i+1]}"
    run_test "$name" "$path" "$c" "$TOTAL"
  done
done

# 汇总报告
summary_entry() {
  local name="$1" c="$2"
  local out="$RESULT_DIR/${name}_c${c}.txt"
  local qps t50 t95 t99 fail
  qps=$(grep "Requests per second" "$out" 2>/dev/null | awk '{print $4}')
  t50=$(grep "50%" "$out" 2>/dev/null | awk '{print $2}')
  t95=$(grep "95%" "$out" 2>/dev/null | awk '{print $2}')
  t99=$(grep "99%" "$out" 2>/dev/null | awk '{print $2}')
  fail=$(grep "Failed requests" "$out" 2>/dev/null | awk '{print $3}')
  printf "%-16s %5s %10s %8s %8s %8s %6s\n" "$name" "$c" "$qps" "${t50}ms" "${t95}ms" "${t99}ms" "$fail"
}
echo "========================================="
echo "  Summary"
echo "========================================="
printf "%-16s %5s %10s %8s %8s %8s %6s\n" "Endpoint" "C" "QPS" "p50" "p95" "p99" "Fail"
printf "%s\n" "$(printf '%.0s-' {1..70})"
for c in $CONCURRENCIES; do
  for ((i=0; i<${#ENDPOINTS[@]}; i+=2)); do
    summary_entry "${ENDPOINTS[$i]}" "$c"
  done
done
echo ""
echo "Full reports: $RESULT_DIR/"
