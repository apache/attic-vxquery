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

(:
Convert xml structure to json structure
:)
let $sensor_collection := "/tmp/1.0_partition_ghcnd_all_xml/sensors"
for $d in collection($sensor_collection)/dataCollection/data
let $date := xs:date(fn:substring(xs:string(fn:data($d/date)), 0, 11))
where $date eq xs:date("2001-01-01") and $d/dataType eq "TMIN"
return 
{
    "results":{
        "date":data($d/date),
        "dataType":data($d/dataType),
        "station":data($d/station),
        "value":data($d/value),
    }
}