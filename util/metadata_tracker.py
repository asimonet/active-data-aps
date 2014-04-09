#! /usr/bin/env python

import os
import sys
import signal
import time
from datetime import datetime
import pprint

pp = pprint.PrettyPrinter(4).pprint

from subprocess import call

sys.path.append("/Users/asimonet/projets/branches/python-catalog-client")

from globusonline.catalog.client import dataset_client
from globusonline.catalog.client.rest_client import RestClientError
from globusonline.catalog.client.operators import Op

dataset_url = "https://catalog-alpha.globuscs.info/service/dataset"
catalog_id = 1

limit = 50

ad_jar = "/Users/asimonet/projets/branches/ActiveData/dist/active-data-lib-0.2.0.jar"
aps_jar = "/Users/asimonet/projets/branches/active-data-aps/target/active-data-aps-1.0-SNAPSHOT.jar"

shutdown = False

def start_tracking(catalog_client):
	last_check = datetime.utcnow()

	while not shutdown:
		success = 0
		failure = 0
		selector = [("modified", Op.GT, last_check.isoformat(' '))]
		selector = [("modified", Op.GT, '2014-03-31 10:00:00+00')]
		#_, data = catalog_client.get_dataset_annotations(catalog_id, selector_list=selector, annotation_list = ['id', 'modified'])
		_, data = catalog_client.get_dataset_annotations(catalog_id, selector_list=selector)
		
		if len(data) > 0:
			pp(data)
		
		for id in data:
			ret = call(["java", \
				"-cp", "%s:%s" % (ad_jar, aps_jar), \
				"org.inria.activedata.examples.cmdline.PublishTransition", \
				"localhost", \
				"-m", "org.inria.activedata.aps.models.APSModel", \
				"-t", "metadata.update", \
				"-sid", "metadata", \
				"-uid", str(id['id'])])
			
			if ret == 0:
				success += 1
			else:
				failure += 1
			
		print "Published update transitions (success/failures): %d/%d" % (success, failure)
		last_check = datetime.utcnow()

		time.sleep(15)
	
	sys.exit(0)
		

def sighandler(signum, frame):
	print "Exiting"
	global shutdown
	shutdown = True
		
# Main code
if len(sys.argv) != 2:
	print "Usage: metadata_tracker.py <goauth token>"
else:
	signal.signal(signal.SIGTERM, sighandler)
	signal.signal(signal.SIGINT, sighandler)

	catalog_client = dataset_client.DatasetClient(sys.argv[1], base_url=dataset_url)
	start_tracking(catalog_client)
