// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#include <vespa/vespalib/io/fileutil.h>
#include <vespa/vespalib/net/tls/transport_security_options.h>
#include <vespa/vespalib/net/tls/transport_security_options_reading.h>
#include <vespa/vespalib/test/peer_policy_utils.h>
#include <vespa/vespalib/testkit/test_kit.h>
#include <vespa/vespalib/util/exceptions.h>

using namespace vespalib;
using namespace vespalib::net::tls;

TEST("can load TLS credentials via config file") {
    auto opts = read_options_from_json_file("ok_config.json");
    ASSERT_TRUE(opts.get() != nullptr);
    // Obviously we'd need to change this to actual PEM data if config reading started
    // actually verifying the _content_ of files, not just reading them.
    EXPECT_EQUAL("My private key\n", opts->private_key_pem());
    EXPECT_EQUAL("My CA certificates\n", opts->ca_certs_pem());
    EXPECT_EQUAL("My certificate chain\n", opts->cert_chain_pem());
}

TEST("missing JSON file throws exception") {
    EXPECT_EXCEPTION(read_options_from_json_file("missing_config.json"), IllegalArgumentException,
                     "TLS config file 'missing_config.json' could not be read");
}

TEST("bad JSON content throws exception") {
    const char* bad_json = "hello world :D";
    EXPECT_EXCEPTION(read_options_from_json_string(bad_json), IllegalArgumentException,
                     "Provided TLS config file is not valid JSON");
}

TEST("missing 'files' field throws exception") {
    const char* incomplete_json = R"({})";
    EXPECT_EXCEPTION(read_options_from_json_string(incomplete_json), IllegalArgumentException,
                     "TLS config root field 'files' is missing or empty");
}

TEST("missing 'private-key' field throws exception") {
    const char* incomplete_json = R"({"files":{"certificates":"dummy_certs.txt","ca-certificates":"dummy_ca_certs.txt"}})";
    EXPECT_EXCEPTION(read_options_from_json_string(incomplete_json), IllegalArgumentException,
                     "TLS config field 'private-key' has not been set");
}

TEST("missing 'certificates' field throws exception") {
    const char* incomplete_json = R"({"files":{"private-key":"dummy_privkey.txt","ca-certificates":"dummy_ca_certs.txt"}})";
    EXPECT_EXCEPTION(read_options_from_json_string(incomplete_json), IllegalArgumentException,
                     "TLS config field 'certificates' has not been set");
}

TEST("missing 'ca-certificates' field throws exception") {
    const char* incomplete_json = R"({"files":{"private-key":"dummy_privkey.txt","certificates":"dummy_certs.txt"}})";
    EXPECT_EXCEPTION(read_options_from_json_string(incomplete_json), IllegalArgumentException,
                     "TLS config field 'ca-certificates' has not been set");
}

TEST("missing file referenced by field throws exception") {
    const char* incomplete_json = R"({"files":{"private-key":"missing_privkey.txt",
                                               "certificates":"dummy_certs.txt",
                                               "ca-certificates":"dummy_ca_certs.txt"}})";
    EXPECT_EXCEPTION(read_options_from_json_string(incomplete_json), IllegalArgumentException,
                     "File 'missing_privkey.txt' referenced by TLS config does not exist");
}

vespalib::string json_with_policies(const vespalib::string& policies) {
    const char* fmt = R"({"files":{"private-key":"dummy_privkey.txt",
                                   "certificates":"dummy_certs.txt",
                                   "ca-certificates":"dummy_ca_certs.txt"},
                          "authorized-peers":[%s]})";
    return vespalib::make_string(fmt, policies.c_str());
}

TransportSecurityOptions parse_policies(const vespalib::string& policies) {
    return *read_options_from_json_string(json_with_policies(policies));
}

TEST("config file without authorized-peers accepts all pre-verified certificates") {
    const char* json = R"({"files":{"private-key":"dummy_privkey.txt",
                                    "certificates":"dummy_certs.txt",
                                    "ca-certificates":"dummy_ca_certs.txt"}})";
    EXPECT_TRUE(read_options_from_json_string(json)->authorized_peers().allows_all_authenticated());
}

// Instead of contemplating what the semantics of an empty allow list should be,
// we do the easy way out and just say it's not allowed in the first place.
TEST("empty policy array throws exception") {
    EXPECT_EXCEPTION(parse_policies(""), vespalib::IllegalArgumentException,
                     "\"authorized-peers\" must either be not present (allows "
                     "all peers with valid certificates) or a non-empty array");
}

TEST("can parse single peer policy with single requirement") {
    const char* json = R"({
      "required-credentials":[
         {"field": "SAN_DNS", "must-match": "hello.world"}
      ]
    })";
    EXPECT_EQUAL(authorized_peers({policy_with({required_san_dns("hello.world")})}),
                 parse_policies(json).authorized_peers());
}

TEST("can parse single peer policy with multiple requirements") {
    const char* json = R"({
      "required-credentials":[
         {"field": "SAN_DNS", "must-match": "hello.world"},
         {"field": "CN", "must-match": "goodbye.moon"}
      ]
    })";
    EXPECT_EQUAL(authorized_peers({policy_with({required_san_dns("hello.world"),
                                                required_cn("goodbye.moon")})}),
                 parse_policies(json).authorized_peers());
}

TEST("unknown field type throws exception") {
    const char* json = R"({
      "required-credentials":[
         {"field": "winnie the pooh", "must-match": "piglet"}
      ]
    })";
    EXPECT_EXCEPTION(parse_policies(json), vespalib::IllegalArgumentException,
                     "Unsupported credential field type: 'winnie the pooh'. Supported are: CN, SAN_DNS");
}

TEST("empty required-credentials array throws exception") {
    const char* json = R"({
      "required-credentials":[]
    })";
    EXPECT_EXCEPTION(parse_policies(json), vespalib::IllegalArgumentException,
                     "\"required-credentials\" array can't be empty (would allow all peers)");
}

// TODO test parsing of multiple policies

TEST_MAIN() { TEST_RUN_ALL(); }

