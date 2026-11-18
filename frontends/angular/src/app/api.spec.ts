import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { api, ApiError, isCredit, UNREACHABLE } from './api'
import { server } from '../test/server'

describe('the API client', () => {
  it('returns the parsed JSON of a successful response', async () => {
    server.use(http.get('/api/thing', () => HttpResponse.json({ name: 'Ledgerline' })))

    await expect(api.get('/api/thing')).resolves.toEqual({ name: 'Ledgerline' })
  })

  it('turns an error response into an ApiError with its message and field errors', async () => {
    server.use(http.post('/api/thing', () => HttpResponse.json(
      { status: 422, message: 'Insufficient funds', fieldErrors: { amount: 'Insufficient funds' } }, { status: 422 })))

    const error = await api.post('/api/thing', {}).catch(e => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 422, message: 'Insufficient funds', fieldErrors: { amount: 'Insufficient funds' } })
  })

  it('still reports an error response that has no JSON body', async () => {
    server.use(http.get('/api/thing', () => new HttpResponse('Bad gateway', { status: 502 })))

    await expect(api.get('/api/thing')).rejects.toMatchObject({ status: 502, message: 'HTTP 502', fieldErrors: {} })
  })

  it('reports a back end it cannot reach as an ApiError the page can show', async () => {
    server.use(http.get('/api/thing', () => HttpResponse.error()))

    await expect(api.get('/api/thing')).rejects.toMatchObject({ status: 0, message: UNREACHABLE })
  })

  it('lets a cancelled request stay cancelled, not a failure', async () => {
    const controller = new AbortController()
    controller.abort()

    const error = (await api.get('/api/thing', controller.signal).catch(e => e)) as Error

    expect(error).not.toBeInstanceOf(ApiError)
    expect(error.name).toBe('AbortError')
  })

  it('returns nothing for 204 No Content', async () => {
    server.use(http.delete('/api/thing', () => new HttpResponse(null, { status: 204 })))

    await expect(api.delete('/api/thing')).resolves.toBeUndefined()
  })

  it.each(['post', 'put', 'patch'] as const)('%s sends its body as JSON', async method => {
    let seen: { method: string; contentType: string | null; body: unknown } | undefined
    server.use(http[method]('/api/thing', async ({ request }) => {
      seen = { method: request.method, contentType: request.headers.get('Content-Type'), body: await request.json() }
      return HttpResponse.json({})
    }))

    await api[method]('/api/thing', { amount: '10.00' })

    expect(seen).toEqual({ method: method.toUpperCase(), contentType: 'application/json', body: { amount: '10.00' } })
  })

  it('asks for JSON and sends no Content-Type when there is no body', async () => {
    let accept: string | null = null
    let contentType: string | null = 'unset'
    server.use(http.get('/api/thing', ({ request }) => {
      accept = request.headers.get('Accept')
      contentType = request.headers.get('Content-Type')
      return HttpResponse.json({})
    }))

    await api.get('/api/thing')

    expect(accept).toBe('application/json')
    expect(contentType).toBeNull()
  })
})

describe('isCredit', () => {
  it('treats deposits and incoming transfers as money in', () => {
    expect(['DEPOSIT', 'TRANSFER_IN'].map(isCredit)).toEqual([true, true])
    expect(['WITHDRAWAL', 'TRANSFER_OUT'].map(isCredit)).toEqual([false, false])
  })
})
