#!/usr/bin/env bash

# params :most_active_users.sh [host] [port]

ExtractMostActiveUsers() {
    echo "Extracting the most active users"
    sudo -su tiamat psql -h localhost -p 5435 -U tiamat -t -A -F"," -c "
    select count(*), changed_by from stop_place  group by changed_by order by count desc
    " > /srv/www/stats/tiamat_most_active_users.csv
}

ExtractMostActiveUsers;