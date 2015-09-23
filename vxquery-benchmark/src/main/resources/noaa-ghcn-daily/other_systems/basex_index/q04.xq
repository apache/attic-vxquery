(: Licensed to the Apache Software Foundation (ASF) under one
   or more contributor license agreements.  See the NOTICE file
   distributed with this work for additional information
   regarding copyright ownership.  The ASF licenses this file
   to you under the Apache License, Version 2.0 (the
   "License"); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing,
   software distributed under the License is distributed on an
   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
   KIND, either express or implied.  See the License for the
   specific language governing permissions and limitations
   under the License. :)

(: XQuery Join Query :)
(: Find all the weather readings for King county for a specific day    :)
(: 1976/7/4.                                                                  :)
let $sensor_collection := "db_sensors"
for $r in db:open($sensor_collection)/root/dataCollection/data

let $station_collection := "db_stations"
for $s in db:open($station_collection)/root/stationCollection/station

where $s/id eq $r/station
    and (some $x in $s/locationLabels satisfies ($x/type eq "ST" and fn:upper-case(fn:data($x/displayName)) eq "WASHINGTON"))
    and xs:dateTime(fn:data($r/date)) eq xs:dateTime("1976-07-04T00:00:00.000")
return $r
