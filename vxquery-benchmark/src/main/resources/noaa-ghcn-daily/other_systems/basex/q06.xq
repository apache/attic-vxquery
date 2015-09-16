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
(: Find the highest recorded temperature (TMAX) for each station for each     :)
(: day over the year 2000.                                                    :)
let $sensor_collection := "../../../../../../../weather_data/dataset-tiny-local/data_links/local_speed_up/d0_p1_i0/sensors/"
for $r in collection($sensor_collection)/root/dataCollection/data

let $station_collection := "../../../../../../../weather_data/dataset-tiny-local/data_links/local_speed_up/d0_p1_i0/stations/"
for $s in collection($station_collection)/root/stationCollection/station

where $s/id eq $r/station
    and $r/dataType eq "TMAX"
    and fn:year-from-dateTime(xs:dateTime(fn:data($r/date))) eq 2000
return ($s/displayName, $r/date, $r/value)