#!/usr/bin/env groovy
/*
    Common groovy methods that can be reused by the pipeline jobs.
*/

import groovy.json.JsonSlurper
import org.jsoup.Jsoup

def jsonToMap(def jsonFile) {
    /*
        Read the JSON file and returns a map object
    */
    def FileExists = sh (returnStatus: true, script: "ls -l ${jsonFile}")
    if (FileExists != 0) {
        println "File {jsonFile} does not exist."
        return [:]
    }
    def props = readJSON file: jsonFile
    return props
}

def getCIMessageMap() {
    /*
        Return the CI_MESSAGE map
    */
    def ciMessage = "${params.CI_MESSAGE}" ?: ""
    if (! ciMessage?.trim() ) {
        return [:]
    }
    def compose = readJSON text: "${params.CI_MESSAGE}"
    return compose
}

def fetchMajorMinorOSVersion(def build_type){
    /*
        method accepts build_type as an input and
        Returns RH-CEPH major version, minor version and OS platform based on build_type
        different build_type supported: compose, osbs, cvp, rc-compose, rc-osbs

    */
    def cimsg = getCIMessageMap()
    def major_ver
    def minor_ver
    def platform

    if (build_type == 'compose' || build_type == 'osbs') {
        major_ver = cimsg.compose_id.substring(7,8)
        minor_ver = cimsg.compose_id.substring(9,10)
        platform = cimsg.compose_id.substring(11,17).toLowerCase()
    }
    if (build_type == 'cvp'){
        major_ver = cimsg.artifact.brew_build_target.substring(5,6)
        minor_ver = cimsg.artifact.brew_build_target.substring(7,8)
        platform = cimsg.artifact.brew_build_target.substring(9,15).toLowerCase()
    }
    if (build_type == 'rc-compose'){
        major_ver = cimsg["compose-id"].substring(7,8)
        minor_ver = cimsg["compose-id"].substring(9,10)
        platform = cimsg["compose-id"].substring(11,17).toLowerCase()
    }
    if (build_type == 'rc-osbs'){
        major_ver = cimsg.tag.name.substring(5,6)
        minor_ver = cimsg.tag.name.substring(7,8)
        platform = cimsg.tag.name.substring(9,15).toLowerCase()
    }
    if major_ver && minor_ver && platform{
        return ["major_version":major_ver, "minor_version":minor_ver, "platform":platform]
    }
    else{
        error "Required value is not obtained.."
    }
}

def fetchCephVersion(def base_url){
    /*
        Fetches ceph version using compose base url
    */
    base_url += "/compose/Tools/x86_64/os/Packages/"
    println base_url
    def document = Jsoup.connect(base_url).get().toString()
    def ceph_ver = document.findAll(/"ceph-common-([\w.-]+)\.([\w.-]+)"/)[0].findAll(/([\d]+)\.([\d]+)\.([\d]+)\-([\d]+)/)
    println ceph_ver
    if (! ceph_ver){
        error "ceph version not found.."
    }
    return ceph_ver[0]
}

def setLock(def major_ver, def minor_ver){
    /*
        create a lock file
    */
    def defaultFileDir = "/ceph/cephci-jenkins/latest-rhceph-container-info"
    def lock_file = "${defaultFileDir}/RHCEPH-${major_ver}.${minor_ver}.lock"
    def lockFileExists = sh (returnStatus: true, script: "ls -l ${lock_file}")
    if (lockFileExists != 0) {
        println "RHCEPH-${major_ver}.${minor_ver}.lock does not exist."
        sh(returnStatus: true, script: "touch ${lock_file}")
    }
    else{
        // sleeping for 10 minutes
        sleep(600)
        def lockFilePresent = sh (returnStatus: true, script: "ls -l ${lock_file}")
        if (lockFilePresent == 0) {
        error "Lock file: RHCEPH-${major_ver}.${minor_ver}.lock already exist.."
        }
    }

}

def unSetLock(def major_ver, def minor_ver){
    /*
        Unset a lock file
    */
    def defaultFileDir = "/ceph/cephci-jenkins/latest-rhceph-container-info"
    def lock_file = "${defaultFileDir}/RHCEPH-${major_ver}.${minor_ver}.lock"
    def lockFileExists = sh (returnStatus: true, script: "ls -l ${lock_file}")
    if (lockFileExists == 0) {
        sh(returnStatus: true, script: "rm -rf ${lock_file}")
    }
}
