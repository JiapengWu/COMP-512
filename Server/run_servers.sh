#!/bin/bash 

#TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=(cs-2 cs-3 cs-4 cs-9)

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Flights 2> Flight_error\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Cars 2> Car_error\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Rooms 2> Room_error\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]}\"" C-m \;
