#!/bin/bash 

#TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=(cs-2 cs-11 cs-4 cs-9)

server_port=1104
middleware_port=1105
tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Flights ${server_port} 2> Flight_error \"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Cars ${server_port} 2> Car_error\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; ./run_server.sh Rooms ${server_port} 2> Rooms_error\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; source ./env_setup.sh; sleep .5s; ./run_tcpMiddleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]} ${middleware_port} ${server_port}\"" C-m \;



