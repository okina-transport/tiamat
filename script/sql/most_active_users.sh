#!/usr/bin/env bash

# params :most_active_users.sh [host] [port]

ExtractMostActiveUsers() {
    echo "Extracting the most active users"
    psql -h localhost -p 5440 -U tiamat -t -A -F"," -c "
    select count(*), changed_by from stop_place  group by changed_by order by count desc
    " > /home/gfora/tiamat_most_active_users.csv
}

ExtractMostActiveUsers;