# -*- coding: utf-8 -*-

#
# * Copyright (c) 2009-2020. Authors: Cytomine SCRLFS.
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *      http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */


__author__ = "Hoyoux Renaud <renaud.hoyoux@cytomine.coop>"
__copyright__ = "Apache 2 license. Made by Cytomine SCRLFS, Belgium, https://www.cytomine.coop/"
__version__ = "1.0.0"

import logging

from cytomine import Cytomine
from cytomine.models import Software, SoftwareParameter, SoftwareProject

# connect to cytomine

cytomine_host = "localhost-core"
cytomine_public_key = "XXX"
cytomine_private_key = "XXX"
id_project = XXX


# Connection to Cytomine Core
Cytomine.connect(cytomine_host, cytomine_public_key, cytomine_private_key, verbose=logging.INFO)

# this is the command line to run the software
execute_command = ("python algo/test_soft/test.py " +
                   "--cytomine_host $host " +
                   "--cytomine_public_key $publicKey " +
                   "--cytomine_private_key $privateKey " +
                   "--cytomine_id_software $cytomine_id_software " +
                   "--cytomine_id_project $cytomine_id_project " +

                   "--cytomine_integer_parameter $cytomine_integer_parameter " +
                   
                   "--log_level INFO")

# define software parameter template
software = Software("Test_Software", "createRabbitJobWithArgsService",
                    "ValidateAnnotation", execute_command).save()
# these two lines are currently mandatory when defining a software
SoftwareParameter("cytomine_id_software", "Number", software.id,  0, True, 0, True).save()
SoftwareParameter("cytomine_id_project", "Number", software.id, 0, True, 1, True).save()


#here declare your parameters
                    #name,                       type, id_software, default_value,required, index
SoftwareParameter("cytomine_integer_parameter", "Number", software.id, None,      True,     10).save()

# add software to a given project
if id_project:
    SoftwareProject(software.id, id_project).save()
