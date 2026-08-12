use scribe_compiler::conformance::fixture;
use serde_json::Value;
use std::{collections::BTreeSet, fs, path::Path};

#[test]
fn every_source_has_one_current_deterministic_fixture() {
    let cases = Path::new("conformance/cases");
    let fixtures = Path::new("conformance/fixtures");

    let case_ids = ids(cases, "source");
    let fixture_ids = ids(fixtures, "json");
    assert_eq!(
        fixture_ids, case_ids,
        "fixtures must exactly cover source cases"
    );

    for id in case_ids {
        let source = fs::read_to_string(cases.join(format!("{id}.source"))).unwrap();
        let fixture_text = fs::read_to_string(fixtures.join(format!("{id}.json"))).unwrap();
        let parsed: Value = serde_json::from_str(&fixture_text).unwrap();
        assert_eq!(parsed, fixture(&id, &source));
        let fixture = parsed;
        assert_eq!(fixture["schemaVersion"], 1);
        assert_eq!(fixture["id"], id);
        assert_eq!(fixture["source"], source);
        assert!(fixture_text.ends_with('\n'));
    }
}

fn ids(directory: &Path, extension: &str) -> BTreeSet<String> {
    fs::read_dir(directory)
        .unwrap_or_else(|error| panic!("cannot read {}: {error}", directory.display()))
        .map(|entry| entry.unwrap().path())
        .filter(|path| path.extension().is_some_and(|value| value == extension))
        .map(|path| path.file_stem().unwrap().to_string_lossy().into_owned())
        .collect()
}
