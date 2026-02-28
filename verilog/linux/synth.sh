#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: ./synth.sh <verilog_file.v>"
  exit 1
fi

input_file="$1"
output_file="$(basename "$input_file").json"

cp "$input_file" ./autoyosys/tmp.v
yosys -s ./autoyosys/auto.ysy
cp ./autoyosys/tmp.json "./$output_file"

echo "Generated: $output_file"
