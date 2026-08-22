function handleKeyDownEvents() {
  const LIST_ENTRY_CLASS = "list-entry";

  const entries = document.querySelectorAll(`.${LIST_ENTRY_CLASS}`);
  let currentEntry = undefined;

  function selectEntry(direction) {
    if (entries.length === 0) {
      return;
    }

    let nextEntry;
    switch (direction) {
      case "down":
        nextEntry = currentEntry === undefined ? 0 : currentEntry + 1;
        break;
      case "up":
        nextEntry = currentEntry === undefined ? entries.length - 1 : currentEntry - 1;
        break;
      default:
        console.error(`unknown direction: ${direction}`);
        return;
    }

    // demote the current entry
    if (currentEntry !== undefined) {
      entries[currentEntry].classList = LIST_ENTRY_CLASS;
    }

    // normalize index and promote the new entry
    currentEntry = Math.abs(entries.length + nextEntry) % entries.length;
    entries[currentEntry].classList = `${LIST_ENTRY_CLASS} selected-entry`;
    entries[currentEntry].scrollIntoView(false);
  }

  function openEntry() {
    if (currentEntry === undefined) {
      return;
    }

    const anchor = entries[currentEntry].querySelector("a");
    if (anchor === null) {
      return;
    }

    anchor.click();
  }

  document.addEventListener("keydown", (event) => {
    switch (event.key) {
      case "j":
        selectEntry("down");
        break;
      case "k":
        selectEntry("up");
        break;
      case "o":
        openEntry();
        break;
    }
  });
}

function run() {
  handleKeyDownEvents();
}

run();
