// Small progressive enhancements. Everything still works with JavaScript disabled,
// except the dashboard's recent activity panel which is loaded from the JSON API.

document.addEventListener('DOMContentLoaded', () => {
    // Ask for confirmation before destructive form submissions (<form data-confirm="...">).
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Submit filter dropdowns as soon as they change.
    document.querySelectorAll('select[data-autosubmit]').forEach(select => {
        select.addEventListener('change', () => select.form.submit());
    });

    setUpMovementForm();
    setUpActivityPanel();
});

function setUpMovementForm() {
    const form = document.getElementById('movement-form');
    if (!form) return;

    const target = document.getElementById('transfer-target');
    const sync = () => {
        const type = form.querySelector('input[name="type"]:checked');
        target.hidden = !type || type.value !== 'TRANSFER';
    };
    form.querySelectorAll('input[name="type"]').forEach(radio => radio.addEventListener('change', sync));
    sync();

    // Show a processing state while the (deliberately slow) server handles the request.
    form.addEventListener('submit', () => {
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = button.dataset.processingText;
        form.setAttribute('aria-busy', 'true');
    });
}

function setUpActivityPanel() {
    const container = document.getElementById('activity');
    if (!container) return;

    const symbol = container.dataset.currencySymbol || '';
    const format = amount => {
        const value = Math.abs(Number(amount)).toLocaleString('en-GB', {
            minimumFractionDigits: 2, maximumFractionDigits: 2
        });
        return symbol + value;
    };
    const labels = {
        DEPOSIT: 'Deposit', WITHDRAWAL: 'Withdrawal', TRANSFER_IN: 'Transfer in', TRANSFER_OUT: 'Transfer out'
    };
    const credit = type => type === 'DEPOSIT' || type === 'TRANSFER_IN';

    const render = entries => {
        if (entries.length === 0) {
            container.innerHTML = '<p class="empty">No activity yet.</p>';
            return;
        }
        const list = document.createElement('ul');
        list.className = 'activity-list';
        list.setAttribute('aria-label', 'Recent activity');
        entries.forEach(entry => {
            const item = document.createElement('li');
            item.dataset.testid = 'activity-item';
            const left = document.createElement('span');
            const link = document.createElement('a');
            link.href = '/accounts/' + entry.accountId;
            link.textContent = entry.accountNickname + ' (' + entry.accountNumber + ')';
            left.append(labels[entry.type] + ' · ' + (entry.description || '') + ' · ', link);
            const amount = document.createElement('span');
            amount.className = 'num ' + (credit(entry.type) ? 'credit' : 'debit');
            amount.textContent = (credit(entry.type) ? '+' : '-') + format(entry.amount);
            item.append(left, amount);
            list.append(item);
        });
        container.replaceChildren(list);
    };

    const load = async () => {
        container.innerHTML = '<p role="status" class="muted">Loading activity…</p>';
        try {
            const response = await fetch(container.dataset.endpoint, {headers: {Accept: 'application/json'}});
            if (!response.ok) throw new Error('HTTP ' + response.status);
            render(await response.json());
        } catch (e) {
            container.innerHTML =
                '<div role="alert" class="flash flash-error">Could not load recent activity. ' +
                'Use Refresh to try again.</div>';
        }
    };

    document.getElementById('refresh-activity').addEventListener('click', load);
    load();
}
