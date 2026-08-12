use scribe_compiler::conformance::fixture;
use std::{env, fs, path::PathBuf, process::ExitCode};

fn main() -> ExitCode {
    match run() {
        Ok(()) => ExitCode::SUCCESS,
        Err(message) => {
            eprintln!("{message}");
            ExitCode::FAILURE
        }
    }
}

fn run() -> Result<(), String> {
    let check = match env::args().skip(1).collect::<Vec<_>>().as_slice() {
        [] => false,
        [flag] if flag == "--check" => true,
        _ => return Err("usage: generate_conformance [--check]".into()),
    };
    let root = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    let cases = root.join("conformance/cases");
    let fixtures = root.join("conformance/fixtures");
    let mut paths = fs::read_dir(&cases)
        .map_err(|error| format!("cannot read {}: {error}", cases.display()))?
        .map(|entry| entry.map(|value| value.path()))
        .collect::<Result<Vec<_>, _>>()
        .map_err(|error| format!("cannot enumerate cases: {error}"))?;
    paths.retain(|path| path.extension().is_some_and(|value| value == "source"));
    paths.sort();

    if !check {
        fs::create_dir_all(&fixtures)
            .map_err(|error| format!("cannot create {}: {error}", fixtures.display()))?;
    }

    let mut stale = Vec::new();
    for path in paths {
        let id = path
            .file_stem()
            .ok_or_else(|| format!("case has no file stem: {}", path.display()))?
            .to_string_lossy()
            .into_owned();
        let source = fs::read_to_string(&path)
            .map_err(|error| format!("cannot read {}: {error}", path.display()))?;
        let rendered = serde_json::to_string_pretty(&fixture(&id, &source))
            .map_err(|error| format!("cannot serialize {id}: {error}"))?
            + "\n";
        let destination = fixtures.join(format!("{id}.json"));
        if check {
            match fs::read_to_string(&destination) {
                Ok(existing) if existing == rendered => {}
                _ => stale.push(destination),
            }
        } else {
            fs::write(&destination, rendered)
                .map_err(|error| format!("cannot write {}: {error}", destination.display()))?;
        }
    }

    if check && !stale.is_empty() {
        return Err(format!(
            "{} conformance fixture(s) missing or stale:\n{}",
            stale.len(),
            stale
                .iter()
                .map(|path| path.display().to_string())
                .collect::<Vec<_>>()
                .join("\n")
        ));
    }
    Ok(())
}
