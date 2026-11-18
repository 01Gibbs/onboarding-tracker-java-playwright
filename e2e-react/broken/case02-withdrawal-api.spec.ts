import { expect, test } from '../solutions/support/fixtures.ts'

test('a withdrawal reduces the balance', async ({ api }) => {
  const { id } = await api.accountWithBalance('100.00')

  await api.withdraw(id, '10.00', 'Cash')

  expect(await api.balanceOf(id)).toBe('90.00')
})
