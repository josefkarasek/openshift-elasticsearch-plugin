/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.elasticsearch.plugin.acl;

import static io.fabric8.elasticsearch.plugin.TestUtils.assertYaml;
import static io.fabric8.elasticsearch.plugin.TestUtils.buildMap;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;

import io.fabric8.elasticsearch.plugin.KibanaIndexMode;
import io.fabric8.elasticsearch.plugin.OpenshiftRequestContextFactory.OpenshiftRequestContext;
import io.fabric8.elasticsearch.plugin.Samples;
import io.fabric8.elasticsearch.plugin.acl.SearchGuardRolesMapping.RolesMapping;

public class SearchGuardRolesMappingACLTest {

    private SearchGuardRolesMapping rolesMapping = new SearchGuardRolesMapping();
    
    
    private OpenshiftRequestContext givenContextFor(String user, boolean isOperations, String...projects) {
        return new OpenshiftRequestContext(user, "", isOperations, new HashSet<>(Arrays.asList(projects)), "abc", KibanaIndexMode.UNIQUE);
    }
    
    private ProjectRolesMappingSyncStrategy givenProjectRolesSyncStrategy() {
        return new ProjectRolesMappingSyncStrategy(rolesMapping, 10);
    }

    private UserRolesMappingSyncStrategy givenUserRolesMappingSyncStrategy() {
        return new UserRolesMappingSyncStrategy(rolesMapping, 15);
    }

    @Test
    public void testGeneratingKibanaUniqueRoleWithOpsUsers() throws Exception {
        RolesMappingSyncStrategy sync = givenProjectRolesSyncStrategy();
        sync.syncFrom(givenContextFor("user1", true));
        sync.syncFrom(givenContextFor("user3", true));
        sync.syncFrom(givenContextFor("user2", false, "foo.bar"));
        
        assertYaml("", Samples.ROLESMAPPING_OPS_SHARED_KIBANA_INDEX_WITH_UNIQUE.getContent(), rolesMapping);
    }

    @Test
    public void testGeneratingKibanaUniqueRoleWithOpsUsersSyncdToUserRolesMappings() throws Exception {
        RolesMappingSyncStrategy sync = givenUserRolesMappingSyncStrategy();
        sync.syncFrom(givenContextFor("user1", true));
        sync.syncFrom(givenContextFor("user3", true));
        sync.syncFrom(givenContextFor("user2.bar@email.com", false, "foo.bar"));
        sync.syncFrom(givenContextFor("CN=jdoe,OU=DL IT,OU=User Accounts,DC=example,DC=com", false, "distinguishedproj"));
        
        assertYaml("", Samples.USER_ROLESMAPPING_STRATEGY.getContent(), rolesMapping);
    }
    
    @Test
    public void testSeserialization() throws Exception {
        List<RolesMapping> mappings = new RolesMappingBuilder()
            .addUser("foo", "user_of_foo")
            .expire("12345")
            .build();
        SearchGuardRolesMapping sgMapping = new SearchGuardRolesMapping();
        sgMapping.addAll(mappings);
        final String out = XContentHelper.toString(sgMapping);
        Map<String, Object> in = XContentHelper.convertToMap(new BytesArray(out), true, XContentType.JSON).v2();
        SearchGuardRolesMapping inMapping = new SearchGuardRolesMapping().load(in);
        assertEquals("Exp serialization to equal derialization", out, XContentHelper.toString(inMapping));
    }

    @Test
    public void testRemove() throws Exception {
        SearchGuardRolesMapping mappings = new SearchGuardRolesMapping()
                .load(buildMap(new StringReader(Samples.ROLESMAPPING_ACL.getContent())));
        for (RolesMapping mapping : mappings) {
            mappings.removeRolesMapping(mapping);
        }
    }

}
