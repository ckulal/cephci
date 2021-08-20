#!/usr/bin/env groovy
/*
    Common groovy methods that can be reused by the pipeline jobs.
*/

import groovy.json.JsonSlurper

def jsonToMap(def jsonFile) {
    /*
        Read the JSON file and returns a map object
    */
    def props = readJSON file: jsonFile
    return props
}

def getCIMessageMap() {
    /*
        Return the CI_MESSAGE map
    */
    def ciMessage = "${params.CI_MESSAGE}" ?: ""
    if (! ciMessage?.trim() ) {
        error "The CI_MESSAGE has not been provided"
    }

    def compose = readJSON text: "${params.CI_MESSAGE}"

    return compose
}

def fetchMajorMinorOSVersion(def build_type){
    def cimsg = getCIMessageMap()
    def major_ver
    def minor_ver
    def os_ver

    if (build_type == 'compose' || build_type == 'osbs') {
        major_ver = cimsg.compose_id.substring(7,8)
        minor_ver = cimsg.compose_id.substring(9,10)
        os_ver = cimsg.compose_id.substring(11,17).toLowerCase()
    }
    if (build_type == 'cvp'){
        major_ver = cimsg.artifact.brew_build_target.substring(5,6)
        minor_ver = cimsg.artifact.brew_build_target.substring(7,8)
        os_ver = cimsg.artifact.brew_build_target.substring(9,15).toLowerCase()
    }
    if (build_type == 'rc-compose'){
        major_ver = cimsg.compose-id.substring(7,8)
        minor_ver = cimsg.compose-id.substring(9,10)
        os_ver = cimsg.compose-id.substring(11,17).toLowerCase()
    }
    if (build_type == 'rc-osbs'){
        major_ver = cimsg.tag.name.substring(5,6)
        minor_ver = cimsg.tag.name.substring(7,8)
        os_ver = cimsg.tag.name.substring(9,15).toLowerCase()
    }
    return ["major_ver":major_ver, "minor_ver":minor_ver, "os_ver":os_ver]

}

def fetchCephVersion(def base_url){
    base_url += "/compose/Tools/x86_64/os/Packages/"
    println base_url
    def document = Jsoup.connect(base_url).get().toString()
    def ceph_ver = document.findAll(/"ceph-common-([\w.-]+)\.([\w.-]+)"/)[0].findAll(/([\d]+)\.([\d]+)\.([\d]+)\-([\d]+)/)
    println ceph_ver
    return ceph_ver[0]
}
