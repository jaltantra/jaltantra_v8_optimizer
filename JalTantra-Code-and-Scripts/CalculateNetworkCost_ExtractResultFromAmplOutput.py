#!/usr/bin/env python3
import os
import re
import subprocess
import sys
from collections import defaultdict
from typing import Dict, List, Tuple, Union

# REFER: https://stackoverflow.com/questions/3172470/actual-meaning-of-shell-true-in-subprocess
g_BASH_PATH = subprocess.check_output(['which', 'bash'], shell=False).decode().strip()


def run_command(cmd: str, default_result: str = '') -> Tuple[bool, str]:
    """
    `stderr` is merged with `stdout`

    Returns:
        Tuple of [ok, output]
    """
    # REFER: Context-Search-fms and CalculateNetworkCost.py for this function
    global g_BASH_PATH
    try:
        # NOTE: Not using the below line of code because "sh" shell does not seem to properly parse the command
        #       Example: `kill -s SIGINT 12345`
        #                did not work and gave the following error:
        #                '/bin/sh: 1: kill: invalid signal number or name: SIGINT'
        #       The error logs of testing has been put in "REPO/logs/2022-01-22_ssh_kill_errors.txt"
        # status_code, output = subprocess.getstatusoutput(cmd)
        output = subprocess.check_output(
            [g_BASH_PATH, '-c', cmd],
            stderr=subprocess.STDOUT,
            shell=False
        ).decode().strip()
        return True, output
    except subprocess.CalledProcessError as e:
        print('DEBUG: Some error occurred, e = ' + str(e), file=sys.stderr)
    return False, default_result


# ---

# Step 1: Validate and store command line arguments in appropriate variables
# print('DEBUG:', sys.argv, file=sys.stderr)
if len(sys.argv) <= 3:
    print('Usage: python3 CalculateNetworkCost_ExtractResultFromAmplOutput.py /path/to/std_out_err.txt '
          '/path/to/NetworkFile PIPE_LEN_THRESHOLD')
    exit(2)
IN_STD_OUT_ERR_FILE_PATH = sys.argv[1]
IN_NETWORK_FILE_PATH = sys.argv[2]
IN_ARC_LEN_ERROR_THRESHOLD = float(sys.argv[3])
#IN_ARC_LEN_ERROR_THRESHOLD = 0
if not os.path.isfile(IN_STD_OUT_ERR_FILE_PATH):
    print(f"ERROR: No such file: '{IN_STD_OUT_ERR_FILE_PATH}'")
    exit(1)
if not os.path.isfile(IN_NETWORK_FILE_PATH):
    print(f"ERROR: No such file: '{IN_NETWORK_FILE_PATH}'")
    exit(1)

# ---

# Step 2: Read the file and extract only the necessary section using `awk`
cmd = """cat '""" + IN_STD_OUT_ERR_FILE_PATH.replace("'", "'\"'\"'") + r"""' | awk '
/^_total_solve_time.*/ { f += f; }
/^h\[i\].*\s*:=/ { f += 1; }
{ if (f==1) print; }
'
"""
ok, ampl_output = run_command(cmd)
if not ok:
    print(f'ERROR: `run_command` failed for {cmd=}', file=sys.stderr)
    print(f'DEBUG: {ampl_output=}', file=sys.stderr)
    exit(1)
del cmd, ok
# print('DEBUG:', f'{ampl_output=}', file=sys.stderr)

# Step 3: Start parsing the file content
calculated_arc_len: Dict[Tuple[int, int], List[List[Union[int, float]]]] = defaultdict(list)
calculated_flow: Dict[Tuple[int, int], int] = defaultdict(int)
calculated_flow_fixed_arcs: Dict[Tuple[int, int], int] = defaultdict(int)
calculated_head: Dict[int, int] = defaultdict(int)

ampl_output = ampl_output.strip().split('\n\n')
# print(f'DEBUG: ampl_output[0]=\n{ampl_output[0]}', file=sys.stderr)
# print(f'DEBUG: ampl_output[1]=\n{ampl_output[1]}', file=sys.stderr)
# print(f'DEBUG: ampl_output[2]=\n{ampl_output[2]}', file=sys.stderr)

# *** Head for node (with NODE_ID=i) = h[i]
# First row is skipped because it contains: `h[i] :=`
for line in ampl_output[0].strip().strip(';').strip().splitlines(keepends=False)[1:]:
    line = line.strip().split()
    # i, h[i]
    line = [int(line[0]), float(line[1])]
    calculated_head[line[0]] = line[1]
# *** Flow for arc=(i,j) = q[i,j]
# First row is skipped because it contains: `q[i,j] :=`
if re.search(r'^q\[i,j\]\s*', ampl_output[1].lstrip()):
    for line in ampl_output[1].strip().strip(';').strip().splitlines(keepends=False)[1:]:
        line = line.strip().split()
        # i, j, q[i,j]
        line = [int(line[0]), int(line[1]), float(line[2])]
        calculated_flow[(line[0], line[1])] = line[2]
    for line in ampl_output[2].strip().strip(';').strip().splitlines(keepends=False)[1:]:
        line = line.strip().split()
        # i, j, q[i,j]
        line = [int(line[0]), int(line[1]), float(line[2])]
        calculated_flow_fixed_arcs[(line[0], line[1])] = line[2]
else:
    for line in ampl_output[1].strip().strip(';').strip().splitlines(keepends=False)[1:]:
        line = line.strip().split()
        line = [int(line[0]), int(line[1]), float(line[2]), float(line[3])]
        if 1e-9 < line[2] and 1e-9 < line[3]:
            print(f'WARNING: Flow variable test depicts some issue, {line=}', file=sys.stderr)
        # else   --->   line[2] <= 1e-6 or line[3] <= 1e-6
        if line[2] <= 1e-9:
            line[2] = 0
        if line[3] <= 1e-9:
            line[3] = 0
        calculated_flow[(line[0], line[1])] = line[2] - line[3]
    for line in ampl_output[2].strip().strip(';').strip().splitlines(keepends=False)[1:]:
        line = line.strip().split()
        line = [int(line[0]), int(line[1]), float(line[2]), float(line[3])]
        if 1e-9 < line[2] and 1e-9 < line[3]:
            print(f'WARNING: Flow variable test depicts some issue, {line=}', file=sys.stderr)
        if line[2] <= 1e-9:
            line[2] = 0
        if line[3] <= 1e-9:
            line[3] = 0
        calculated_flow_fixed_arcs[(line[0], line[1])] = line[2] - line[3]

# *** Length of the pipe (with PIPE_ID=k) used for arc=(i,j) = l[i,j,k]
# First row is skipped because it contains: `l[i,j,k] :
for line in ampl_output[3].strip().strip(';').strip().splitlines(keepends=False)[1:]:
    line = line.strip().split()
    # i, j, k, l[i,j,k]
    line = [int(line[0]), int(line[1]), int(line[2]), float(line[3])]
    calculated_arc_len[(line[0], line[1])].append([line[2], line[3]])

del ampl_output, line

# ---

# NOTE: Not sure if the checking done in step 4 and step 5 is required or not. I think that to some extent
#       it is required because, due to rounding errors in float numbers, things like `20.000001 + 80.000001`
#       can result in sum being not "exactly equal to" the expected arc length (arc is same as an edge in graph).
#       Search for "# Sample OUTPUT" in this file to see an example

# Step 4: NOTE: Read `expected_arc_len` from network file (`IN_NETWORK_FILE_PATH`) which was
#               used to get ".../std_out_err.txt" and validate `calculated_arc_len`

ok, output = run_command("cat '{}'".format(IN_NETWORK_FILE_PATH.replace("'", "'\"'\"'")))
if not ok:
    print('ERROR: `run_command` failed for cmd:\n\t\t' +
          "cat '{}'".format(IN_NETWORK_FILE_PATH.replace("'", "'\"'\"'")), file=sys.stderr)
    print(f'DEBUG: {output=}', file=sys.stderr)
    exit(1)
network_file_data = output.splitlines(keepends=False)
del ok, output

arcs_table_start_flag = False
omega = 10.68
diameter_table_start_flag = False
roughness_table_start_flag = False
pressure_table_start_flag = False
elevation_table_start_flag = False
pressure_dict = {}
elevation_dict = {}
diameter_list = []
roughness_list = []
source_node_of_network = None
expected_arc_len: Dict[Tuple[int, int], float] = dict()

# Sub-step 4.2 : Get Diameter data for constraint checking :
for line in network_file_data:
    line = line.strip()
    cols = line.rstrip(';').split()
    if diameter_table_start_flag:
        diameter_list.append(float(cols[1]))
        if ';' in line:
            diameter_table_start_flag = False
    if roughness_table_start_flag:
        roughness_list.append(float(cols[1]))
        if ';' in line:
            roughness_table_start_flag = False
    if pressure_table_start_flag:
        pressure_dict[int(cols[0])] = float(cols[1])
        if ';' in line:
            pressure_table_start_flag = False
    if elevation_table_start_flag:
        elevation_dict[int(cols[0])] = float(cols[1])
        if ';' in line:
            elevation_table_start_flag = False

    # Regular expression checking for Diameter ->
    if type(line) is str and re.search(r'param\s*d', line):
        diameter_table_start_flag = True
        continue
    # Regular expression checking for roughness ->
    if type(line) is str and re.search(r'param\s*R', line):
        roughness_table_start_flag = True
        continue
    # Regular expression checking for Pressure ->
    if type(line) is str and re.search(r'param\s*P', line):
        pressure_table_start_flag = True
        continue
    # Regular expression checking for Elevation ->
    if type(line) is str and re.search(r'param\s*E', line):
        elevation_table_start_flag = True
        continue
    if type(line) is str and re.search(r'param\s*Source', line):
        source_node_of_network = int(cols[3])
        continue

# Getting expected arc lengths :
for line in network_file_data:
    line = line.strip()
    if arcs_table_start_flag:
        if len(line) > 2:
            cols = line.rstrip(';').split()
            expected_arc_len[(int(cols[0]), int(cols[1]),)] = float(cols[2])
        if ';' in line:
            break
    # NOTE: Blank lines will automatically get skipped, no action will be taken for them
    # print('DEBUG:', f'{line=}', file=sys.stderr)
    # REFER: https://stackoverflow.com/questions/9012008/pythons-re-return-true-if-string-contains-regex-pattern
    if type(line) is str and re.search(r'param\s*:\s*arcs\s*:\s*L', line) and ";" not in line :
        arcs_table_start_flag = True
        continue

del network_file_data, arcs_table_start_flag, line, diameter_table_start_flag, roughness_table_start_flag

# Step 5: Check if all constraints are satisfied :
# Step 5.1 : Constraint-1 (FLow constraint) ->
# sum{i in nodes : (i,j) in arcs}q[i,j] = sum{i in nodes : (j,i) in arcs}q[j,i] + D[j];

# Step 5.2 : Constraint-2 (Min. Pressure constraint) -> h[i] >= E[i] + P[i];
for node_id, head in calculated_head.items():
    if head < pressure_dict[node_id] - elevation_dict[node_id]:
        print( f'ERROR Violation of Min. Pressure Constraint with {node_id=}, {head=}, '
               f'pressure = {pressure_dict[node_id]}, '
               f'elevation = {elevation_dict[node_id]=}', file=sys.stderr)
        exit(1)

# Step 5.4 : Constraint-4 (Length constraint) ->  sum{k in pipes} l[i,j,k] = L[i,j];
# Also fix the rounding error issues due to floating point numbers here
for arc, pipes in calculated_arc_len.items():
    pipe_len_sum = sum([pipe_len for pipe_id, pipe_len in pipes])
    if abs(expected_arc_len[arc] - pipe_len_sum) > IN_ARC_LEN_ERROR_THRESHOLD:
        print(f'ERROR with {arc=}, {pipes=}, {pipe_len_sum=}, {expected_arc_len[arc]=}', file=sys.stderr)
        print(f'ERROR: DEBUG: {expected_arc_len=}', file=sys.stderr)
        print(f'ERROR: DEBUG: {calculated_arc_len=}', file=sys.stderr)
        exit(1)
    if pipe_len_sum != expected_arc_len[arc]:
        print(f'INFO : FIXING: {arc=}, {pipes=}, {pipe_len_sum=}, {expected_arc_len[arc]=}', file=sys.stderr)
        calculated_arc_len[arc][-1][-1] += (expected_arc_len[arc] - pipe_len_sum)
del expected_arc_len

# Step 5.5 : Constraint-5 (Source constraint) -> h[Source] = E[Source]
if calculated_head[source_node_of_network] != elevation_dict[source_node_of_network]:
    print(f'ERROR Violation of Source Constraint with {arc=}, Head  = {head[source_node_of_network]}, '
          f'elevation = {elevation_dict[source_node_of_network]}', file=sys.stderr)
    exit(1)

# Step 5.3 : Constraint-3 (Head loss constraint) ->
# h[i] - h[j] = (q[i,j]*abs(q[i,j])^0.852)*(0.001^1.852)*sum{k in pipes}omega*l[i,j,k]/((R[k]^1.852)*(d[k]/1000)^4.87);
for arc, pipes in calculated_arc_len.items():
    head_source = calculated_head[arc[0]]
    head_destination = calculated_head[arc[1]]
    left_hand_side = float(head_source - head_destination)
    flow_inbetween = calculated_flow[arc]
    right_hand_side = 0.0
    for pipe_id, pipe_len in pipes:
        right_hand_side += (flow_inbetween * (abs(flow_inbetween)**0.852) * (0.001 ** 1.852) * omega * pipe_len) / ((roughness_list[pipe_id]**1.852) * (diameter_list[pipe_id]/1000)**4.87)
    if abs(left_hand_side - right_hand_side) > IN_ARC_LEN_ERROR_THRESHOLD:
        print(f'ERROR Violation of Head Constraint with {arc=}, Head difference = {left_hand_side}, '
              f'Head Loss =  {right_hand_side}', file=sys.stderr)
        exit(1)

# Step 6: Print the output in a format similar to Competitive Programming
#         question for further prteessing by the caller of this program
# print('DEBUG:', calculated_head, file=sys.stderr)
# print('DEBUG:', calculated_flow, file=sys.stderr)
# print('DEBUG:', calculated_arc_len, file=sys.stderr)
# Print `head`
print(len(calculated_head))
for i_node, j_head in calculated_head.items():
    print(i_node, j_head)
# Print `flow`
print(len(calculated_flow) + len(calculated_flow_fixed_arcs))
for i_arc, j_flow in calculated_flow.items():
    print(i_arc[0], i_arc[1], j_flow)
for i_arc, j_flow in calculated_flow_fixed_arcs.items():
    print(i_arc[0], i_arc[1], j_flow)
# Print `pipes` and their `lengths` for each `arc`
print(len(calculated_arc_len) + len(calculated_flow_fixed_arcs))  # NUMBER_OF_ARCS_ie_EDGES (Both fixed & unfixed)
for i_arc, j_pipes_list in calculated_arc_len.items(): # First handle unfixed arcs
    # Print -> ARC_SOURCE_VERTEX, ARC_DESTINATION_VERTEX, OPTIMAL_NUMBER_OF_PIPES_REQUIRED
    print(i_arc[0], i_arc[1], len(j_pipes_list))
    for pipe_id, pipe_len in j_pipes_list:
        # Print -> PIPE_ID, PIPE_LENGTH
        print(pipe_id, pipe_len)
for i_arc, j_flow in calculated_flow_fixed_arcs.items(): # Handling fixed arcs
    # Fixed arcs already have predetermined pipe so the below lines handle result extraction of fixed_arcs
    print(i_arc[0], i_arc[1], 0)


"""

# --------------------------------------------------


# Sample INPUT
NOTE: The below table was kept as a reference for developing the above parsing algorithm

output = '''
h[i] [*] :=
1  210
2  203.244
3  196.69
4  198.981
5  192.108
6  195
7  190
;

q[i,j] :=
1 2   311.109
2 3   158.043
2 4   125.288
3 5   130.266
4 5     0.157062
4 6    91.7983
6 7     0.132273
7 5   -55.4227
;

l[i,j,k] :=
1 2 11   1000
2 3 9    1000
2 4 9    1000
3 5 9    1000
4 5 1    1000
4 6 8     591.257
4 6 9     408.743
6 7 1    1000
7 5 7      40.0913
7 5 8     959.909
;
'''


# --------------------------------------------------


# Sample INPUT
NOTE: The below table was kept as a reference for developing the above parsing algorithm

output = '''
h[i] [*] :=
1  210
2  203.244
3  196.69
4  198.981
5  192.108
6  195
7  190
;

:        q1[i,j]        q2[i,j]       :=
1 2   311.109          0
2 3   158.043          1.00155e-12
2 4   125.288          1.00205e-12
3 5   130.266          1.00196e-12
4 5     0.157062       2.75667e-12
4 6    91.7983         1.00292e-12
6 7     0.132273       3.39767e-12
7 5     1.00507e-12   55.4227
;

l[i,j,k] :=
1 2 11   1000
2 3 9    1000
2 4 9    1000
3 5 9    1000
4 5 1    1000
4 6 8     591.257
4 6 9     408.743
6 7 1    1000
7 5 7      40.0913
7 5 8     959.909
;
'''


# --------------------------------------------------


# Sample OUTPUT

# NOTE: both of the below commands mean the same
# (dev) ➜  Jaltantra-Code-and-Scripts python CalculateNetworkCost_ExtractResultFromAmplOutput.py '/home/student/VirtualBox VMs/VM_Desktop/mtp/NetworkResults/e8df08dacdff232cc9e1f70869324438/octeract_m2_e8df08dacdff232cc9e1f70869324438/std_out_err.txt' '/home/student/VirtualBox VMs/VM_Desktop/mtp/NetworkResults/e8df08dacdff232cc9e1f70869324438/0_graph_network_data_testcase.R' 1
# (dev) ➜  Jaltantra-Code-and-Scripts python CalculateNetworkCost_ExtractResultFromAmplOutput.py ~/Desktop/tempout.out "/home/student/VirtualBox VMs/VM_Desktop/mtp/Files/Data/m1_m2/d1_Sample_input_cycle_twoloop.dat" 1

# NOTE: both of the below commands mean the same
# (dev) ➜  Jaltantra-Code-and-Scripts python CalculateNetworkCost_ExtractResultFromAmplOutput.py '/home/student/VirtualBox VMs/VM_Desktop/mtp/NetworkResults/e8df08dacdff232cc9e1f70869324438/octeract_m2_e8df08dacdff232cc9e1f70869324438/std_out_err.txt' '/home/student/VirtualBox VMs/VM_Desktop/mtp/NetworkResults/e8df08dacdff232cc9e1f70869324438/0_graph_network_data_testcase.R' 1
# (dev) ➜  Jaltantra-Code-and-Scripts python CalculateNetworkCost_ExtractResultFromAmplOutput.py ~/Desktop/tempout.out "/home/student/VirtualBox VMs/VM_Desktop/mtp/Files/Data/m1_m2/d1_Sample_input_cycle_twoloop.dat" 1

INFO : FIXING: arc=(7, 5), pipes=[[7, 40.0913], [8, 959.909]], pipe_len_sum=1000.0003, expected_arc_len[arc]=1000.0
DEBUG: defaultdict(<class 'int'>, {1: 210.0, 2: 203.244, 3: 196.69, 4: 198.981, 5: 192.108, 6: 195.0, 7: 190.0})
DEBUG: defaultdict(<class 'int'>, {(1, 2): 311.109, (2, 3): 158.043, (2, 4): 125.288, (3, 5): 130.266, (4, 5): 0.157062, (4, 6): 91.7983, (6, 7): 0.132273, (7, 5): -55.4227})
DEBUG: defaultdict(<class 'list'>, {(1, 2): [[11, 1000.0]], (2, 3): [[9, 1000.0]], (2, 4): [[9, 1000.0]], (3, 5): [[9, 1000.0]], (4, 5): [[1, 1000.0]], (4, 6): [[8, 591.257], [9, 408.743]], (6, 7): [[1, 1000.0]], (7, 5): [[7, 40.0913], [8, 959.9087]]})
7
1 210.0
2 203.244
3 196.69
4 198.981
5 192.108
6 195.0
7 190.0
8
1 2 311.109
2 3 158.043
2 4 125.288
3 5 130.266
4 5 0.157062
4 6 91.7983
6 7 0.132273
7 5 -55.4227
8
1 2 1
11 1000.0
2 3 1
9 1000.0
2 4 1
9 1000.0
3 5 1
9 1000.0
4 5 1
1 1000.0
4 6 2
8 591.257
9 408.743
6 7 1
1 1000.0
7 5 2
7 40.0913
8 959.9087


# --------------------------------------------------

"""
