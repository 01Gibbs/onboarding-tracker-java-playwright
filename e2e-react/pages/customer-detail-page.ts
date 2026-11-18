import type { Locator, Page } from '@playwright/test'
import { NavBar } from './nav-bar.ts'

/** A single customer at /customers/{id}. */
export class CustomerDetailPage {
  constructor(private readonly page: Page) {}

  async open(customerId: number): Promise<CustomerDetailPage> {
    throw new Error('TODO lesson 07')
  }

  get nav(): NavBar {
    throw new Error('TODO lesson 07')
  }

  get heading(): Locator {
    throw new Error('TODO lesson 07')
  }

  get flash(): Locator {
    throw new Error('TODO lesson 07')
  }

  /** The value shown for a term in the details list, e.g. detail('Email'). */
  detail(term: string): Locator {
    throw new Error('TODO lesson 07: each term and value is a group named by its term')
  }

  /** Clicks "Delete customer" and accepts the browser's confirm dialog. */
  async deleteAndConfirm(): Promise<void> {
    throw new Error('TODO lesson 07')
  }
}
