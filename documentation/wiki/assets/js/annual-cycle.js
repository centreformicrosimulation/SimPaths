(() => {
  let mounted = null;
  let dispose = () => {};

  const setup = () => {
    const explorer = document.querySelector('[data-annual-explorer]');
    // Material also emits for same-page anchors: retain the reader's selection.
    if (explorer && explorer === mounted) return;
    dispose();
    dispose = () => {};
    mounted = explorer;
    if (!explorer) return;

    const stages = [...explorer.querySelectorAll('.annual-explorer__stage')];
    const container = explorer.querySelector('.annual-explorer__stages');
    if (!container || !stages.length) return;

    const controller = new AbortController();
    const tabs = document.createElement('div');
    tabs.className = 'annual-explorer__tabs';
    tabs.setAttribute('role', 'tablist');
    tabs.setAttribute('aria-label', 'Modules in the published cycle');
    tabs.setAttribute('aria-orientation', 'vertical');
    const pickerLabel = document.createElement('label');
    pickerLabel.className = 'annual-explorer__picker';
    pickerLabel.append(document.createTextNode('Module'));
    const picker = document.createElement('select');
    pickerLabel.append(picker);

    const buttons = stages.map((stage, index) => {
      const summary = stage.querySelector(':scope > summary');
      const panel = stage.querySelector('.annual-explorer__content');
      const button = document.createElement('button');
      button.type = 'button';
      button.id = `${stage.id}-tab`;
      button.className = 'annual-explorer__tab';
      button.textContent = summary.textContent.trim();
      button.setAttribute('role', 'tab');
      button.setAttribute('aria-controls', `${stage.id}-panel`);
      panel.id = `${stage.id}-panel`;
      panel.setAttribute('role', 'tabpanel');
      panel.setAttribute('aria-labelledby', button.id);
      panel.tabIndex = 0;
      tabs.append(button);
      const option = document.createElement('option');
      option.value = String(index);
      option.textContent = button.textContent;
      picker.append(option);
      return button;
    });

    const select = (index) => {
      picker.value = String(index);
      stages.forEach((stage, position) => {
        const selected = position === index;
        stage.open = selected;
        stage.hidden = !selected;
        buttons[position].setAttribute('aria-selected', String(selected));
        buttons[position].tabIndex = selected ? 0 : -1;
      });
    };

    picker.addEventListener('change', () => select(Number(picker.value)), { signal: controller.signal });

    buttons.forEach((button, index) => {
      button.addEventListener('click', () => select(index), { signal: controller.signal });
      button.addEventListener('keydown', (event) => {
        let next = index;
        if (event.key === 'ArrowDown') next = (index + 1) % buttons.length;
        else if (event.key === 'ArrowUp') next = (index + buttons.length - 1) % buttons.length;
        else if (event.key === 'Home') next = 0;
        else if (event.key === 'End') next = buttons.length - 1;
        else return;
        event.preventDefault();
        // Panels are local and immediate, so arrow keys activate as well as focus.
        select(next);
        buttons[next].focus();
      }, { signal: controller.signal });
    });

    container.prepend(pickerLabel, tabs);
    explorer.classList.add('annual-explorer--enhanced');
    select(0);

    dispose = () => {
      controller.abort();
      tabs.remove();
      pickerLabel.remove();
      explorer.classList.remove('annual-explorer--enhanced');
      stages.forEach((stage) => {
        stage.hidden = false;
        stage.open = false;
        const panel = stage.querySelector('.annual-explorer__content');
        ['role', 'aria-labelledby', 'tabindex', 'id'].forEach(attribute => panel.removeAttribute(attribute));
      });
      mounted = null;
    };
  };

  if (typeof document$ !== 'undefined') document$.subscribe(setup);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setup, { once: true });
  else setup();
})();
