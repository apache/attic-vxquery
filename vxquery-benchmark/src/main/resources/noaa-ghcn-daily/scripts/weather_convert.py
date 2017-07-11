#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from abc import ABCMeta, abstractmethod

import textwrap
from datetime import date
import os
from collections import OrderedDict

# Custom modules.
from weather_config_ghcnd import *
from weather_config_mshr import *
from weather_download_files import *

ext = ""

class WeatherConvert(object):

    __metaclass__ = ABCMeta

    STATES = OrderedDict({
        'AK': 'Alaska',
        'AL': 'Alabama',
        'AR': 'Arkansas',
        'AS': 'American Samoa',
        'AZ': 'Arizona',
        'CA': 'California',
        'CO': 'Colorado',
        'CT': 'Connecticut',
        'DC': 'District of Columbia',
        'DE': 'Delaware',
        'FL': 'Florida',
        'GA': 'Georgia',
        'GU': 'Guam',
        'HI': 'Hawaii',
        'IA': 'Iowa',
        'ID': 'Idaho',
        'IL': 'Illinois',
        'IN': 'Indiana',
        'KS': 'Kansas',
        'KY': 'Kentucky',
        'LA': 'Louisiana',
        'MA': 'Massachusetts',
        'MD': 'Maryland',
        'ME': 'Maine',
        'MI': 'Michigan',
        'MN': 'Minnesota',
        'MO': 'Missouri',
        'MP': 'Northern Mariana Islands',
        'MS': 'Mississippi',
        'MT': 'Montana',
        'NA': 'National',
        'NC': 'North Carolina',
        'ND': 'North Dakota',
        'NE': 'Nebraska',
        'NH': 'New Hampshire',
        'NJ': 'New Jersey',
        'NM': 'New Mexico',
        'NV': 'Nevada',
        'NY': 'New York',
        'OH': 'Ohio',
        'OK': 'Oklahoma',
        'OR': 'Oregon',
        'PA': 'Pennsylvania',
        'PR': 'Puerto Rico',
        'RI': 'Rhode Island',
        'SC': 'South Carolina',
        'SD': 'South Dakota',
        'TN': 'Tennessee',
        'TX': 'Texas',
        'UT': 'Utah',
        'VA': 'Virginia',
        'VI': 'Virgin Islands',
        'VT': 'Vermont',
        'WA': 'Washington',
        'WI': 'Wisconsin',
        'WV': 'West Virginia',
        'WY': 'Wyoming'
    })
    
    MONTHS = [
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    ]
    
    token = ""

    def __init__(self, base_path, save_path, debug_output):
        self.save_path = save_path
        self.debug_output = debug_output

        # Extra support files.
        self.ghcnd_countries = base_path + '/ghcnd-countries.txt'
        self.ghcnd_inventory = base_path + '/ghcnd-inventory.txt'
        self.ghcnd_states = base_path + '/ghcnd-states.txt'
        self.ghcnd_stations = base_path + '/ghcnd-stations.txt'
        
        # MSHR support files.
        self.mshr_stations = base_path + '/mshr_enhanced_201607.txt'
        
    def set_token(self, token):
        self.token = token
        
    def get_field_from_definition(self, row, field_definition):
        return row[(field_definition[FIELD_INDEX_START] - 1):field_definition[FIELD_INDEX_END]]
    
    def get_field(self, fields_array, row, index):
        return row[(fields_array[index][FIELD_INDEX_START] - 1):fields_array[index][FIELD_INDEX_END]]
    
    def get_dly_field(self, row, index):
        return self.get_field(DLY_FIELDS, row, index)
    
    def print_row_files(self, row):
        for field in DLY_FIELDS:
            print str(field[FIELD_INDEX_NAME]) + " = '" + row[(field[FIELD_INDEX_START] - 1):field[FIELD_INDEX_END]] + "'"
    
    def save_file(self, filename, contents):
        file = open(filename, 'w')
        file.write(contents)
        file.close()
        return filename
    
    def get_folder_size(self, folder_name):
        total_size = 0
        for dirpath, dirnames, filenames in os.walk(folder_name):
            for f in filenames:
                fp = os.path.join(dirpath, f)
                total_size += os.path.getsize(fp)
        return total_size

    def process_one_month_sensor_set(self, records, page):
        # Default
        return 0
    
    def process_station_data(self, row):
        # Default
        return 0

    def get_base_folder(self, station_id, data_type="sensors"):
        return build_base_save_folder(self.save_path, station_id, data_type) 
    
    def process_inventory_file(self):
        print "Processing inventory file"
        file_stream = open(self.ghcnd_inventory, 'r')
        
        csv_header = ['ID', 'SENSORS', 'SENSORS_COUNT', 'MAX_YEARS', 'TOTAL_YEARS_FOR_ALL_SENSORS']
        row = file_stream.readline()
        csv_inventory = {}
        for row in file_stream:
            id = self.get_field_from_definition(row, INVENTORY_FIELDS['ID'])
            sensor_id = self.get_field_from_definition(row, INVENTORY_FIELDS['ELEMENT'])
            start = int(self.get_field_from_definition(row, INVENTORY_FIELDS['FIRSTYEAR']))
            end = int(self.get_field_from_definition(row, INVENTORY_FIELDS['LASTYEAR']))
            if id in csv_inventory:
                new_count = str(int(csv_inventory[id][2]) + 1)
                new_max = str(max(int(csv_inventory[id][3]), (end - start)))
                new_total = str(int(csv_inventory[id][3]) + end - start)
                csv_inventory[id] = [id, (csv_inventory[id][1] + "," + sensor_id), new_count, new_max, new_total]
            else:
                csv_inventory[id] = [id, sensor_id, str(1), str(end - start), str(end - start)]
                
        path = self.save_path + "/inventory.csv"
        self.save_csv_file(path, csv_inventory, csv_header)
    
    def save_csv_file(self, path, csv_inventory, header):
        csv_content = "|".join(header) + "\n"
        for row_id in csv_inventory:
            csv_content += "|".join(csv_inventory[row_id]) + "\n"
        self.save_file(path, csv_content)
        

    def process_station_file(self, file_name):
        print "Processing station file: " + file_name
        file_stream = open(file_name, 'r')
        
        row = file_stream.readline()
        return self.process_station_data(row)

    def process_sensor_file(self, file_name, max_files, sensor_max=99):
        print "Processing sensor file: " + file_name
        file_stream = open(file_name, 'r')
    
        month_last = 0
        year_last = 0
        records = []
        page = 0
        sensor_count = 0
    
        file_count = 0
        for row in file_stream:
            month = self.get_dly_field(row, DLY_FIELD_MONTH)
            year = self.get_dly_field(row, DLY_FIELD_YEAR)
            
            if (month_last != 0 and year_last != 0) and (sensor_count >= sensor_max or month != month_last or year != year_last):
                # process set
                file_count += self.process_one_month_sensor_set(records, page)
                records = []
                if sensor_count >= sensor_max and month == month_last and year == year_last:
                    # start a new page.
                    page += 1
                else:
                    # start over.
                    page = 0
                sensor_count = 0
            
            records.append(row)
            sensor_count += 1
            if max_files != 0 and file_count >= max_files:
                # Stop creating more files after the max is reached.
                break

            month_last = month
            year_last = year
        
        station_id = self.get_dly_field(records[0], DLY_FIELD_ID)
        data_size = self.get_folder_size(self.get_base_folder(station_id) + "/" + station_id)
        print "Created " + str(file_count) + self.extension +" files for a data size of " + str(data_size) + "."
        
        return (file_count, data_size)
    
    def convert_c2f(self, c):
        return (9 / 5 * c) + 32
    
    @abstractmethod
    def default_web_service_start(self):
        pass
    
    @abstractmethod
    def default_data_start(self, total_records):
        pass

    @abstractmethod
    def default_station_start(self):
        pass
    

    @abstractmethod 
    def default_field_date(self, report_date, indent=2):
        pass
    
    def default_mshr_station_additional(self, station_id):
        """The web service station data is generate from the MSHR data supplemented with GHCN-Daily."""
        station_mshr_row = ""
        stations_mshr_file = open(self.mshr_stations, 'r')
        for line in stations_mshr_file:
            if station_id == self.get_field_from_definition(line, MSHR_FIELDS['GHCND_ID']).strip():
                station_mshr_row = line
                break
        
        if station_mshr_row == "":
            return ""

        additional= ""

        county = self.get_field_from_definition(station_mshr_row, MSHR_FIELDS['COUNTY']).strip()
        if county != "":
            additional += self.default_location_labels("CNTY", "FIPS:-9999", county)
            
        country_code = self.get_field_from_definition(station_mshr_row, MSHR_FIELDS['FIPS_COUNTRY_CODE']).strip()
        country_name = self.get_field_from_definition(station_mshr_row, MSHR_FIELDS['FIPS_COUNTRY_NAME']).strip()
        if country_code != "" and country_name != "":
            additional += self.default_location_labels("CNTRY", "FIPS:" + country_code, country_name)
        
        return additional

    @abstractmethod
    def default_location_labels(self, type, id, display_name):
        pass
        

    @abstractmethod 
    def default_web_service_station(self, station_id):
        """The web service station data is generate from available historical sources."""
        pass

    @abstractmethod        
    def default_day_reading_as_field(self, row, day):
        pass
    
    @abstractmethod 
    def default_day_reading(self, row, day, indent=2):
        pass
    
    def default_end(self):
        return textwrap.dedent("""\
            </ghcnd_observation>""")

    def default_data_end(self):
        return self.default_end_tag("dataCollection", 0)

    def default_station_end(self):
        return self.default_end_tag("stationCollection", 0)

    @abstractmethod
    def default_element(self, tag, data, indent=1):
        pass 

    @abstractmethod 
    def default_start_tag(self, tag, indent=1):
	pass
 
    @abstractmethod 
    def default_end_tag(self, tag, indent=1):
	pass

    def get_indent_space(self, indent):
        return (" " * (4 * indent))

   # Station data
    def process_station_data(self, row):
        """Adds a single station record file either from downloading the data or generating a similar record."""
        station_id = self.get_dly_field(row, DLY_FIELD_ID)
        download = 0
        if self.token is not "" and not self.skip_downloading:
            download = self.download_station_data(station_id, self.token, True)
            if download == 0:
                self.skip_downloading = True
        
        # If not downloaded, generate.
        if download != 0:
            return download
        else:
            # Information for each daily file.
            station_file = self.default_web_service_start()
            station_file = self.default_station_start()
            station_file += self.default_web_service_station(station_id)
            station_file += self.default_station_end()
            
            # Remove white space.
            station_file = station_file.replace("\n", "");
            station_file = station_file.replace(self.get_indent_space(1), "");
            station_file = station_file.replace(",]}", "]}")

            # Make sure the station folder is available.
            ghcnd_station_path = self.get_base_folder(station_id, "stations")
            if not os.path.isdir(ghcnd_station_path):
                os.makedirs(ghcnd_station_path)
                    
            # Save XML string to disk.
            save_file_name = ghcnd_station_path + station_id + self.ext
            save_file_name = self.save_file(save_file_name, station_file)
    
            if save_file_name is not "":
                if self.debug_output:
                    print "Wrote file: " + save_file_name
                return 1
            else:
                return 0 

    # Station data
    def download_station_data(self, station_id, token, reset=False):
        """Downloads the station data from the web service."""
        import time
        time.sleep(2)
        # Make sure the station folder is available.
        ghcnd_station_path = self.get_base_folder(station_id, "stations")
        if not os.path.isdir(ghcnd_station_path):
            os.makedirs(ghcnd_station_path)
                
        # Build download URL.
        url = "http://www.ncdc.noaa.gov/cdo-services/services/datasets/GHCND/stations/GHCND:" + station_id + ".xml?token=" + token
        url_file = urllib.urlopen(url)
        station_file = ""
        while (True):
            line = url_file.readline()
            if not line:
                break
            station_file += line
        
        if station_file.find("<cdoError>") != -1:
            if self.debug_output:
                print "Error in station download"
            return 0
        
        # Save XML string to disk.
        save_file_name = ghcnd_station_path + station_id + self.ext
        save_file_name = self.save_file(save_file_name, station_file)
    
        if save_file_name is not "":
            if self.debug_output:
                print "Wrote file: " + save_file_name
            return 2
        else:
            return 0 

    @abstractmethod
    def process_one_month_sensor_set(records, page):
        pass

def build_base_save_folder(save_path, station_id, data_type="sensors"):
    # Default
    station_prefix = station_id[:3]
    return save_path + data_type + "/" + station_prefix + "/"

def build_sensor_save_filename(ext, station_id, report_date, page):
    # Default
    return station_id + "_" + str(report_date.year).zfill(4) + str(report_date.month).zfill(2) + "_" + str(page) + ext


class WeatherWebServiceMonthlyJSONFile(WeatherConvert):
    """The web service class details how to create files similar to the NOAA web service."""
    skip_downloading = False
    extension = "JSON"
    ext=".json"

    def default_web_service_start(self):
        field = ""
        return field

    def default_data_start(self, total_records):
        field = ""
        field += "{\"metadata\":{\"resultset\":{\"offset\":1, \"count\":" + str(total_records) + "\"limit\":25}}\n"
        return field
    
    def default_station_start(self):
        field = ""
        field = "{\"metadata\":{\"resultset\":{\"offset\":1, \"count\":1, \"limit\":25}}\n"
        return field
    
    def default_field_date(self, report_date, indent=2):
        field = ""
        field += self.get_indent_space(indent) + "\"date\":" + "\"" + str(report_date.year) + "-" + str(report_date.month).zfill(2) + "-" + str(report_date.day).zfill(2) + "T00:00:00.000\",\n"
        return field

    def default_location_labels(self, type, id, display_name):
        label = ""
        label += self.default_start_tag("locationLabels", 2)
        label += self.default_element("type", "\"" + type + "\",", 3)
        label += self.default_element("id", "\"" +id + "\",", 3)
        label += self.default_element("displayName", "\"" + display_name + "\"", 3)
        label += self.default_end_tag("locationLabels", 2)
        return label
        

    def default_web_service_station(self, station_id):
        """The web service station data is generate from available historical sources."""
        station_ghcnd_row = ""
        stations_ghcnd_file = open(self.ghcnd_stations, 'r')
        for line in stations_ghcnd_file:
            if station_id == self.get_field_from_definition(line, STATIONS_FIELDS['ID']):
                station_ghcnd_row = line
                break
    
        station = ","
        station += self.default_start_tag("results", 1)
        
        station += "{" + self.default_element("id", "\"GHCND:" + station_id + "\",", 2)
        station += self.default_element("displayName", "\"" + self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['NAME']).strip() + "\",", 2)
        station += self.default_element("latitude", self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['LATITUDE']).strip() + ",", 2)
        station += self.default_element("longitude", self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['LONGITUDE']).strip() + ",", 2)
        
        elevation = self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['ELEVATION']).strip()
        if elevation != "-999.9":
            station += self.default_element("elevation", elevation + ",", 2)
        
        state_code = self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['STATE']).strip()
        if state_code != "" and state_code in self.STATES:
            station += self.default_location_labels("ST", "FIPS:" + str(self.STATES.keys().index(state_code)), self.STATES[state_code])
        
        # Add the MSHR data to the station generated information.
        station += self.default_mshr_station_additional(station_id) + "},"
            
        station += self.default_end_tag("results", 1)
        #station = station.replace(",]}", "]}")
        return station
        
    def default_day_reading_as_field(self, row, day):
        day_index = DLY_FIELD_DAY_OFFSET + ((day - 1) * DLY_FIELD_DAY_FIELDS)
        value = self.get_dly_field(row, day_index);
        if value == "-9999":
            return ""
    
        field = ""
        field_id = self.get_dly_field(row, DLY_FIELD_ELEMENT)
        if field_id in ("MDTN", "MDTX", "MNPN", "MXPN", "TMAX", "TMIN", "TOBS",):
            # Add both the celcius and fahrenheit temperatures.
            celcius = float(value) / 10
            field += "            \"" + field_id + "_c\":" + str(celcius) + "\n" 
            fahrenheit = self.convert_c2f(celcius)
            field += "            \"" + field_id + "_f\":" + str(fahrenheit) + "\n" 
        elif field_id in ("AWND", "EVAP", "PRCP", "THIC", "WESD", "WESF", "WSF1", "WSF2", "WSF5", "WSFG", "WSFI", "WSFM",):
            # Field values that are in tenths.
            converted_value = float(value) / 10
            field += "            \"" + field_id + "\":" + str(converted_value) + "\n"
        elif field_id in ("ACMC", "ACMH", "ACSC", "ACSH", "PSUN",):
            # Fields is a percentage.
            field += "            \"" + field_id + "\":" + value.strip() + "\n" 
        elif field_id in ("FMTM", "PGTM",):
            # Fields is a time value HHMM.
            field += "            \"" + field_id + "\":" + value.strip() + "\n"
        elif field_id in ("DAEV", "DAPR", "DASF", "DATN", "DATX", "DAWM", "DWPR", "FRGB", "FRGT", "FRTH", "GAHT", "MDSF", "MDWM", "MDEV", "MDPR", "SNOW", "SNWD", "TSUN", "WDF1", "WDF2", "WDF5", "WDFG", "WDFI", "WDFM", "WDMV",):
            # Fields with no alternation needed.
            field += "            \"" + field_id + "\":" + value.strip() + "\n"
        else:
            field += "            \"unknown\":" + field_id + "\n"
            
        # print field
        return field
    
    def default_day_reading(self, row, day, indent=2):
        day_index = DLY_FIELD_DAY_OFFSET + ((day - 1) * DLY_FIELD_DAY_FIELDS)
        value = self.get_dly_field(row, day_index);
        mflag = self.get_dly_field(row, day_index + 1);
        qflag = self.get_dly_field(row, day_index + 2);
        sflag = self.get_dly_field(row, day_index + 3);
        mFlag = mflag.strip()
        qFlag = qflag.strip()
        sFlag = sflag.strip()
  
        if value == "-9999":
            return ""

        indent_space = self.get_indent_space(indent)
        field_id = self.get_dly_field(row, DLY_FIELD_ELEMENT)
        station_id = "\"GHCND:" + self.get_dly_field(row, DLY_FIELD_ID)+"\""
    
        field = ""
        field += indent_space + "\"dataType\":" + "\"" + field_id + "\"" + ",\n"
        field += indent_space + "\"station\":" + station_id + ",\n"
        field += indent_space + "\"value\":" + value.strip() + ",\n"
        field += indent_space + "\"attributes\":\"" + mFlag + qFlag + sFlag + ",\"\n"
        #field += indent_space + indent_space + "\"attribute\":" + mflag.strip() + qflag.strip() + sflag.strip() + ",\n"
        #field += indent_space + indent_space + "\"attribute\":" + qflag.strip() + ",\n"
        #field += indent_space + indent_space + "\"attribute\":" + sflag.strip() + ",\n"
        #field += indent_space + indent_space + "\"attribute\":null\n"
        #field += indent_space + "}\n"
        #field += indent_space + "}\n"
       # if mFlag == "":
        #    mFlag = mFlag.replace("", ",")
      #  if qFlag == "":
       #     qFlag = qFlag.replace("", ",")
       # if sFlag == "":
       #     sFlag = sFlag.replace("", ",")

	#	field = field.replace("", ",")
        # print field
        return field   

    def default_element(self, tag, data, indent=1):
        return self.get_indent_space(indent) + "\"" + tag + "\":" + data 

    def default_start_tag(self, tag, indent=1):
	if tag == "locationLabels":
		return self.get_indent_space(indent) + "\"" + tag + "\":{"
	elif tag == "results":
		return self.get_indent_space(indent) + "{\"" + tag + "\":["
	else:
        	return self.get_indent_space(indent) + "{\"" + tag + "\":{"

    def default_end_tag(self, tag, indent=1):
	if tag == "results":
		return self.get_indent_space(indent) + "]}" + "\n"
	else:
        	return self.get_indent_space(indent) + "}" + "\n" 

    # Sensor data
    def process_one_month_sensor_set(self, records, page):
        """Generates records for a station using the web service xml layout."""
        found_data = False        
        year = int(self.get_dly_field(records[0], DLY_FIELD_YEAR))
        month = int(self.get_dly_field(records[0], DLY_FIELD_MONTH))
    
        station_id = self.get_dly_field(records[0], DLY_FIELD_ID)

        # Information for each daily file.
        count = 0
        daily_file = ","
	daily_file += self.default_start_tag("results")
        
        for day in range(1, 32):
            try:
                # TODO find out what is a valid python date range? 1889?
                # Attempt to see if this is valid date.
                report_date = date(year, month, day)

                for record in records:
                    record_snip = self.default_day_reading(record, report_date.day)
                    if record_snip is not "":
			daily_file += "{"
                        daily_file += self.default_field_date(report_date)
                        daily_file += record_snip
			daily_file += "},"
                        found_data = True
                        count += 1

            except ValueError:
                pass
	
	daily_file += self.default_end_tag("results")
	#daily_file = daily_file.replace(",]}", "]}")
        daily_file = self.default_data_start(count) + daily_file + self.default_data_end()
        daily_file = daily_file.replace("\n", "");
        daily_file = daily_file.replace(self.get_indent_space(1), "");
	daily_file = daily_file.replace(",]}", "]}")

        if not found_data:
            return 0

        # Make sure the station folder is available.
        ghcnd_station_path = self.get_base_folder(station_id) + "/" + station_id + "/" + str(report_date.year) + "/"
        if not os.path.isdir(ghcnd_station_path):
            os.makedirs(ghcnd_station_path)
                
        # Save XML string to disk.
        save_file_name = ghcnd_station_path + build_sensor_save_filename(".json", station_id, report_date, page)
        save_file_name = self.save_file(save_file_name, daily_file)

        if save_file_name is not "":
            if self.debug_output:
                print "Wrote file: " + save_file_name
            return 1
        else:
            return 0


class WeatherWebServiceMonthlyXMLFile(WeatherConvert):

    """The web service class details how to create files similar to the NOAA web service."""
    skip_downloading = False
    extension = "XML"
    ext=".xml"

    def default_web_service_start(self):
        field = ""
        field += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        return field

    def default_data_start(self, total_records):
        field = ""
        field += "<dataCollection pageCount=\"1\" totalCount=\"" + str(total_records) + "\">\n"
        return field
    
    def default_station_start(self):
        field = ""
        field = "<stationCollection pageSize=\"100\" pageCount=\"1\" totalCount=\"1\">\n"
        return field
    
    def default_field_date(self, report_date, indent=2):
        field = ""
        field += self.get_indent_space(indent) + "<date>" + str(report_date.year) + "-" + str(report_date.month).zfill(2) + "-" + str(report_date.day).zfill(2) + "T00:00:00.000</date>\n"
        return field

    def default_location_labels(self, type, id, display_name):
        label = ""
        label += self.default_start_tag("locationLabels", 2)
        label += self.default_element("type", type, 3)
        label += self.default_element("id", id, 3)
        label += self.default_element("displayName", display_name, 3)
        label += self.default_end_tag("locationLabels", 2)
        return label
        

    def default_web_service_station(self, station_id):
        """The web service station data is generate from available historical sources."""
        station_ghcnd_row = ""
        stations_ghcnd_file = open(self.ghcnd_stations, 'r')
        for line in stations_ghcnd_file:
            if station_id == self.get_field_from_definition(line, STATIONS_FIELDS['ID']):
                station_ghcnd_row = line
                break
    
        station = ""
        station += self.default_start_tag("station", 1)
        
        station += self.default_element("id", "GHCND:" + station_id, 2)
        station += self.default_element("displayName", self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['NAME']).strip(), 2)
        station += self.default_element("latitude", self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['LATITUDE']).strip(), 2)
        station += self.default_element("longitude", self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['LONGITUDE']).strip(), 2)
        
        elevation = self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['ELEVATION']).strip()
        if elevation != "-999.9":
            station += self.default_element("elevation", elevation, 2)
        
        state_code = self.get_field_from_definition(station_ghcnd_row, STATIONS_FIELDS['STATE']).strip()
        if state_code != "" and state_code in self.STATES:
            station += self.default_location_labels("ST", "FIPS:" + str(self.STATES.keys().index(state_code)), self.STATES[state_code])
        
        # Add the MSHR data to the station generated information.
        station += self.default_mshr_station_additional(station_id)
            
        station += self.default_end_tag("station", 1)
        return station
        
    def default_day_reading_as_field(self, row, day):
        day_index = DLY_FIELD_DAY_OFFSET + ((day - 1) * DLY_FIELD_DAY_FIELDS)
        value = self.get_dly_field(row, day_index);
        if value == "-9999":
            return ""
    
        field = ""
        field_id = self.get_dly_field(row, DLY_FIELD_ELEMENT)
        if field_id in ("MDTN", "MDTX", "MNPN", "MXPN", "TMAX", "TMIN", "TOBS",):
            # Add both the celcius and fahrenheit temperatures.
            celcius = float(value) / 10
            field += "            <" + field_id + "_c>" + str(celcius) + "</" + field_id + "_c>\n"
            fahrenheit = self.convert_c2f(celcius)
            field += "            <" + field_id + "_f>" + str(fahrenheit) + "</" + field_id + "_f>\n"
        elif field_id in ("AWND", "EVAP", "PRCP", "THIC", "WESD", "WESF", "WSF1", "WSF2", "WSF5", "WSFG", "WSFI", "WSFM",):
            # Field values that are in tenths.
            converted_value = float(value) / 10
            field += "            <" + field_id + ">" + str(converted_value) + "</" + field_id + ">\n"
        elif field_id in ("ACMC", "ACMH", "ACSC", "ACSH", "PSUN",):
            # Fields is a percentage.
            field += "            <" + field_id + ">" + value.strip() + "</" + field_id + ">\n"
        elif field_id in ("FMTM", "PGTM",):
            # Fields is a time value HHMM.
            field += "            <" + field_id + ">" + value.strip() + "</" + field_id + ">\n"
        elif field_id in ("DAEV", "DAPR", "DASF", "DATN", "DATX", "DAWM", "DWPR", "FRGB", "FRGT", "FRTH", "GAHT", "MDSF", "MDWM", "MDEV", "MDPR", "SNOW", "SNWD", "TSUN", "WDF1", "WDF2", "WDF5", "WDFG", "WDFI", "WDFM", "WDMV",):
            # Fields with no alternation needed.
            field += "            <" + field_id + ">" + value.strip() + "</" + field_id + ">\n"
        else:
            field += "            <unknown>" + field_id + "</unknown>\n"
            
        # print field
        return field
    
    def default_day_reading(self, row, day, indent=2):
        day_index = DLY_FIELD_DAY_OFFSET + ((day - 1) * DLY_FIELD_DAY_FIELDS)
        value = self.get_dly_field(row, day_index);
        mflag = self.get_dly_field(row, day_index + 1);
        qflag = self.get_dly_field(row, day_index + 2);
        sflag = self.get_dly_field(row, day_index + 3);

        if value == "-9999":
            return ""

        indent_space = self.get_indent_space(indent)
        field_id = self.get_dly_field(row, DLY_FIELD_ELEMENT)
        station_id = "GHCND:" + self.get_dly_field(row, DLY_FIELD_ID)
    
        field = ""
        field += indent_space + "<dataType>" + field_id + "</dataType>\n"
        field += indent_space + "<station>" + station_id + "</station>\n"
        field += indent_space + "<value>" + value.strip() + "</value>\n"
        field += indent_space + "<attributes>\n"
        field += indent_space + indent_space + "<attribute>" + mflag.strip() + "</attribute>\n"
        field += indent_space + indent_space + "<attribute>" + qflag.strip() + "</attribute>\n"
        field += indent_space + indent_space + "<attribute>" + sflag.strip() + "</attribute>\n"
        field += indent_space + indent_space + "<attribute></attribute>\n"
        field += indent_space + "</attributes>\n"

        # print field
        return field
    
    def default_element(self, tag, data, indent=1):
        return self.get_indent_space(indent) + "<" + tag + ">" + data + "</" + tag + ">\n"

    def default_start_tag(self, tag, indent=1):
        return self.get_indent_space(indent) + "<" + tag + ">\n"

    def default_end_tag(self, tag, indent=1):
        return self.get_indent_space(indent) + "</" + tag + ">\n"

    def process_one_month_sensor_set(self, records, page):
        """Generates records for a station using the web service xml layout."""
        found_data = False        
        year = int(self.get_dly_field(records[0], DLY_FIELD_YEAR))
        month = int(self.get_dly_field(records[0], DLY_FIELD_MONTH))
    
        station_id = self.get_dly_field(records[0], DLY_FIELD_ID)

        # Information for each daily file.
        count = 0
        daily_file = ""
        
        for day in range(1, 32):
            try:
                # TODO find out what is a valid python date range? 1889?
                # Attempt to see if this is valid date.
                report_date = date(year, month, day)

                for record in records:
                    record_snip = self.default_day_reading(record, report_date.day)
                    if record_snip is not "":
                        daily_file += self.default_start_tag("data")
                        daily_file += self.default_field_date(report_date)
                        daily_file += record_snip
                        daily_file += self.default_end_tag("data")
                        found_data = True
                        count += 1

            except ValueError:
                pass

        daily_file = self.default_web_service_start() + self.default_data_start(count) + daily_file + self.default_data_end()
        daily_file = daily_file.replace("\n", "");
        daily_file = daily_file.replace(self.get_indent_space(1), "");

        if not found_data:
            return 0

        # Make sure the station folder is available.
        ghcnd_station_path = self.get_base_folder(station_id) + "/" + station_id + "/" + str(report_date.year) + "/"
        if not os.path.isdir(ghcnd_station_path):
            os.makedirs(ghcnd_station_path)
                
        # Save XML string to disk.
        save_file_name = ghcnd_station_path + build_sensor_save_filename(".xml", station_id, report_date, page)
        save_file_name = self.save_file(save_file_name, daily_file)

        if save_file_name is not "":
            if self.debug_output:
                print "Wrote file: " + save_file_name
            return 1
        else:
            return 0
