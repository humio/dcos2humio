package com.humio.mesos.dcos2humio.scheduler.service;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class CsvStringToMapConverterTest {

    CsvStringToMapConverter converter = new CsvStringToMapConverter();

    @Test
    public void canParseEmptyString() {
        assertThat(converter.convert("")).isEmpty();
    }
    @Test
    public void canParseSimpleSingleKv() {
        assertThat(converter.convert("foo:bar")).containsExactly(entry("foo", "bar"));
    }

    @Test
    public void canParseListedKvs() {
        assertThat(converter.convert("fee:fie;foe:foo")).containsExactly(entry("fee", "fie"), entry("foe", "foo"));
    }

    @Test
    public void canParseWhiteSpaceListedKvs() {
        assertThat(converter.convert(" fee: fie; foe:foo")).containsExactly(entry("fee", "fie"), entry("foe", "foo"));
    }

    @Test
    public void canParseKvsWithDelimiterAtEnd() {
        assertThat(converter.convert("fee:fie; ")).containsExactly(entry("fee", "fie"));
    }

    @Test
    public void canParseKvsWithEmptyKey() {
        assertThat(converter.convert("fee:fie;:foo")).containsExactly(entry("fee", "fie"));
    }

    @Test
    public void canParseKvsWithEmptyValue() {
        assertThat(converter.convert("fee:fie;foe:")).containsExactly(entry("fee", "fie"), entry("foe", ""));
    }

    @Test @Ignore("One day…")
    public void canParseQuotedListedKvs() {
        assertThat(converter.convert("fee:\"fie\";\"foe\":\":foo;\"")).containsExactly(entry("fee", "fie"), entry("foe", ":foo;"));
    }
}