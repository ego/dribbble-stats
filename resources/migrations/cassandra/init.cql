CREATE KEYSPACE "api_octopus" WITH REPLICATION = {
'class': 'NetworkTopologyStrategy', 'datacenter1': 1};

CREATE TABLE "api_octopus"."token" (
code text PRIMARY KEY,
count_call int,
available_from timestamp);

CREATE INDEX on "api_octopus"."token" (count_call);
CREATE INDEX on "api_octopus"."token" (available_from);

BEGIN BATCH
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('856cefc88b08243c168f5c8c1daf48461cf736d24a8da8699a91eb7f9129fc30', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('6a1d5bcbfd4246e05dc7e16393b44d44467c59e82095cf30871f672f6532c3cf', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('494f1f9c2a4f7bd7c4e3f3eb3664a5e9615cdf08e9d03e23bb3f3565d298902e', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('c2f373f1f85295b8a6972e046b62a3a2cea24af4138b3f729f6dfd75a412cbfa', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('ea1b76d005b72e45b5b2a58e086aca2afa3d5cf1fad8d6e4e798a5acaada8c24', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('c73eebfb13a23d78f94cb2fa9ce81696eefc391b5af2fd7857cf044401c9cd02', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('47a69aff3b6ede26ce5258886fd1f1a3cae576c2f842431d01ec60a7b44893f0', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('d65e33ddbe4c027403c1a7b7f6cd52a3bf1a1f3d2a89d492a2a53d60c2112c70', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('d62f4ee9902976b65fd8e6b12565dee3c14e9ba1725f11746718ae841b63506e', 0, toTimestamp(now()));
INSERT INTO "api_octopus"."token" (code, count_call, available_from) VALUES ('ee973c6489dc11eb0fe0b57d374521438f5a1fa86c064e9014331d608fca9751', 0, toTimestamp(now()));
APPLY BATCH;

CREATE TABLE "api_octopus"."user_likers" (
message_id uuid,
username text,
liker_count int,
likername text,
created_at timestamp,
PRIMARY KEY (message_id, username, likername));

CREATE INDEX on "api_octopus"."user_likers" (liker_count);
CREATE INDEX on "api_octopus"."user_likers" (created_at);

-- MATERIALIZED VIEW
CREATE MATERIALIZED VIEW "api_octopus"."user_likers_count" AS
SELECT * FROM "api_octopus"."user_likers"
WHERE username IS NOT NULL AND liker_count IS NOT NULL AND likername IS NOT NULL AND message_id IS NOT NULL
PRIMARY KEY ((message_id, username), liker_count, likername);

-- SELECT * FROM "api_octopus"."user_likers_count" WHERE username = 'mpazienza' AND message_id = 89ab46a7-9daa-4c41-8075-4f65181cf545 ORDER BY liker_count DESC ALLOW FILTERING;

-- MATERIALIZED VIEW
CREATE MATERIALIZED VIEW "api_octopus"."user_likers_username" AS
SELECT * FROM "api_octopus"."user_likers"
WHERE username IS NOT NULL AND liker_count IS NOT NULL AND likername IS NOT NULL AND message_id IS NOT NULL
PRIMARY KEY (username, likername, liker_count, message_id);

-- SELECT * FROM "api_octopus"."user_likers_username" WHERE username = 'mpazienza' ORDER BY likername desc LIMIT 10 ALLOW FILTERING;

CREATE MATERIALIZED VIEW "api_octopus"."user_likers_likername" AS
SELECT * FROM "api_octopus"."user_likers"
WHERE username IS NOT NULL AND liker_count IS NOT NULL AND likername IS NOT NULL AND message_id IS NOT NULL
PRIMARY KEY ((likername, username, message_id), liker_count);

-- SELECT * FROM "api_octopus"."user_likers_likername" WHERE likername = 'mpazienza' ORDER BY liker_count desc LIMIT 10 ALLOW FILTERING;

-- UDF/UDA

-- GROUP BY field AND GET MAX value
-- CREATE FUNCTION api_octopus.state_group_and_max(state map<text, int>, type text, amount int)
-- CALLED ON NULL INPUT
-- RETURNS map<text, int>
-- LANGUAGE java AS
-- 'Integer count = (Integer) state.get(type);
-- if (count == null) count = amount;
-- else count = Math.max(count, amount);
-- state.put(type, count);
-- return state; ';

-- CREATE OR REPLACE AGGREGATE api_octopus.group_and_max(text, int)
-- SFUNC state_group_and_max
-- STYPE map<text, int>
-- INITCOND {};

-- SELECT group_and_max(likername, liker_count) FROM api_octopus.user_likers WHERE username = 'foo';
