#!/usr/bin/env python

import sys
import os
import operator

from subprocess import call

#############################
# Some constants
#############################
ad_jar = "/Users/asimonet/projets/branches/ActiveData/dist/active-data-lib-0.2.0.jar"
aps_jar = "/Users/asimonet/projets/branches/active-data-aps/target/active-data-aps-1.0-SNAPSHOT.jar"

if len(sys.argv) != 3:
	sys.exit("Usage: %s <ad_host> <run_directory>" % sys.argv[0])

ad_host = sys.argv[1]

# Open log file
log_directory = os.path.normpath(sys.argv[2])
log_filename = os.path.join(log_directory, os.path.basename(log_directory) + ".log")
log_file = open(log_filename, "r")

# Class definition for a single Task
class Task:
	app			= ''
	arguments	= ''
	host		= ''
	stageIn		= ''
	stageOut	= ''
	startTime	= ''
	stopTime	= ''
	taskNumber	= ''
	thread		= ''
	workdir		= ''
	success		= ''

# Dictionary containing all tasks
tasks = {}
taskCounter = 1

# Retrieve Task from dictionary, or create new
def getTask(taskid):
	if taskid in tasks:
		return tasks[taskid]
	else:
		t = Task()
		tasks[taskid] = t
		return tasks[taskid]

# In a log entry, find values that start with value=<nnn>
def getValue(entry, value):
	entry_array = entry.split()
	value += '='
	for word in entry_array:
		if word.startswith(value):
			return word.split(value, 1)[1]

# Get timestamp of a log entry
def getTime(entry):
   timestamp = entry.split()[1]
   return timestamp.split(',')[0]

# Get all text between [ and ]
def getBracketedText(entry):
   return entry.partition('[')[-1].rpartition(']')[0]

# Print tasks
def printTask(t):
	print "Task: %s" % t.taskNumber
	print "\tApp name: %s" % t.app
	print "\tCommand line arguments: %s" % t.arguments
	print "\tHost: %s" % t.host
	print "\tStart time: %s" % t.startTime
	print "\tStop time: %s" % t.stopTime
	print "\tWork directory: %s" % t.workdir
	print "\tStaged in: %s" % t.stageIn
	print "\tStaged out: %s\n" % t.stageOut
	print "\tSuccess: %s\n" % t.success

def taskSuccess(t, outputPath):
	if t.stageIn is None:
		return
	
	inputPath = t.stageIn
	if inputPath.startswith("__root__"):
		inputPath = inputPath[8:]

	if outputPath.startswith("__root__"):
		outputPath = outputPath[8:]

	call(["java", \
		"-cp", "%s:%s" % (ad_jar, aps_jar), \
		"org.inria.activedata.examples.cmdline.PublishTransition", \
		ad_host, \
		"-m", "org.inria.activedata.aps.models.APSModel", \
		"-t", "swift.derive", \
		"-sid", "swift", \
		"-uid", inputPath, \
		"-newId", outputPath])

def taskFailure(t):
	inputPath = t.stageIn
	if inputPath.startswith("__root__"):
		inputPath = inputPath[8:]

	call(["java", \
		"-cp", "%s:%s" % (ad_jar, aps_jar), \
		"org.inria.activedata.examples.cmdline.PublishTransition", \
		ad_host, \
		"-m", "org.inria.activedata.aps.models.APSModel", \
		"-t", "swift.failure", \
		"-sid", "swift", \
		"-uid", inputPath])
	
def stageIn(files):
	for file in files:
		call(["java", \
		"-cp", "%s:%s" % (ad_jar, aps_jar), \
		"org.inria.activedata.examples.cmdline.PublishTransition", \
		ad_host, \
		"-m", "org.inria.activedata.aps.models.APSModel", \
		"-t", "swift.initialize", \
		"-sid", "swift", \
		"-uid", file])

# Parse log
for line in iter(log_file):
	if 'JOB_START' in line:
		taskid			= getValue(line, "jobid")
		task			= getTask(taskid)
		task.app		= getValue(line, "tr")
		task.startTime	= getTime(line)
		task.workdir	= getValue(line, "tmpdir")
		task.host		= getValue(line, "host")
		task.arguments	= getBracketedText(line)
		task.taskNumber	= taskCounter
		taskCounter		= taskCounter+1

	elif 'JOB_END' in line:
		taskid			= getValue(line, "jobid")
		task			= getTask(taskid)
		task.stopTime	= getTime(line)

	elif "Staging in files" in line:
		taskid			= getValue(line, "jobid")
		task			= getTask(taskid)
		task.stageIn	= getBracketedText(line)
		stageIn(task.stageIn.split(' '))

	elif "FILE_STAGE_OUT_START" in line:
		taskid			= getValue(line, "jobid")
		task			= getTask(taskid)
		file_out		= getValue(line, "srcname")
		task.stageOut  += file_out + " "
		taskSuccess(task, file_out)
	
	elif "SUCCESS" in line:
		taskid			= getValue(line, "jobid")
		if taskid is None:
			continue

		task			= getTask(taskid)
		task.success	= True
	
	elif "FAILURE" in line:
		taskid			= getValue(line, "jobid")
		if taskid is None:
			continue

		task			= getTask(taskid)
		task.success	= False
		taskFailure(task)
