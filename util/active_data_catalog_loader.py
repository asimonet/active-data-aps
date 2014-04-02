#! /usr/bin/env python

import os
import sys
import json
from subprocess import call

sys.path.append("/Users/asimonet/projets/branches/python-catalog-client")

from globusonline.catalog.client import dataset_client
from globusonline.catalog.client.rest_client import RestClientError
from globusonline.catalog.client.operators import Op

dataset_url="https://catalog-alpha.globuscs.info/service/dataset"
catalog_id = 1

ep_name = "asimonet#aps"
ep_path = "/~/"

ad_jar = "/Users/asimonet/projets/branches/ActiveData/dist/active-data-lib-0.2.0.jar"
aps_jar = "/Users/asimonet/projets/branches/active-data-aps/target/active-data-aps-1.0-SNAPSHOT.jar"

def create_dataset(catalog_client, name, file_metadata):
     _, data = catalog_client.create_dataset(catalog_id,{ "name": name })
     dataset_id = data["id"]
     #_, data =  catalog_client.add_dataset_annotations(catalog_id, dataset_id,metadata)
     #print "Added dataset %s" % name
     members = []
     for file_name, metadata in file_metadata.items():
            #print metadata
	    metadata['data_type'] = "file"
            metadata['data_uri'] = "globus://%s/%s" %(ep_name, file_name)
	    _, data = catalog_client.create_member(catalog_id, dataset_id,metadata)
            member_id = data['id']
	    members.append(dict(file_name= member_id))
     dataset_created(dataset_id, members, name)
     return dataset_id

def process_dir(root, catalog_client):
    print "Processing %s" % root
    for dirname, dirs, files in os.walk(root):
        for d in dirs:
	    #print d
	    members = {} 
	    for _, _, fs in os.walk(os.path.join(root, d)): 
	    	for f in fs: 
		     members[f] ={'fname': f, 
				  'fsize': os.path.getsize(os.path.join(root, d,f))}
            create_dataset(catalog_client, d, members)

def get_modified_time(dataset_id, catalog_client):
    _, data = catalog_client.get_dataset_annotations(catalog_id, dataset_id, ['modified'])
    last_modified(dataset_id, data[0]['modified'])


# functions to call external application
def dataset_created(dataset_id, members, name):
    call(["java", \
    	"-cp", "%s:%s" % (ad_jar, aps_jar), \
    	"org.inria.activedata.examples.cmdline.PublishTransition", \
    	"localhost", \
    	"-m", "org.inria.activedata.aps.models.APSModel", \
    	"-t", "APS.extract", \
    	"-sid", "APS", \
    	"-uid", os.path.join(ep_path, name), \
    	"-newId", str(dataset_id)])
    	

def last_modified(dataset_id, time):
    call(["echo", "Dataset %s last modified at %s" %(dataset_id, time)])



if len(sys.argv) < 2:
    print "Not enough args"
if (sys.argv[1] == "modified"):
    print "Getting last modified time for id %s" % sys.argv[3]
    catalog_client = dataset_client.DatasetClient(sys.argv[2], base_url=dataset_url)
    get_modified_time(sys.argv[3], catalog_client)
else:
    print "Creating a new dataset for root dir %s" % sys.argv[1]
    catalog_client = dataset_client.DatasetClient(sys.argv[2], base_url=dataset_url)
    ep_path = os.path.join(ep_path, sys.argv[1])
    process_dir(sys.argv[1], catalog_client)
