#!/bin/bash 

#TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=(cs-2 cs-3 cs-4 cs-5 cs-9)

server_port=1100
middleware_port=1101
tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Flights ${server_port}\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Cars ${server_port}\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Rooms ${server_port}\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t ${MACHINES[4]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]} ${MACHINES[3]} ${middleware_port}\"" C-m \;
